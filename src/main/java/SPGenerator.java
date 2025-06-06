import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import Behaviour.Behaviour;
import Behaviour.Utils;
import Behaviour.Cdt;
import jakarta.json.*;
import Behaviour.End;
import Behaviour.Comm;

public class SPGenerator implements Generator{
    int nodes;
    JsonObject rules;
    String rulesFile = "rules_valid.json";
    HashMap<String, Requirement> requirements = new HashMap<>();
    GenerationContext currentCtx;
    HashMap<String, Behaviour> system = new HashMap<>();
    static int commCounter = 0;

    SPGenerator(int nodes){
        this.nodes = nodes;
    }

    SPGenerator(int nodes, String rulesFile){
        this(nodes);
        this.rulesFile = rulesFile;
    }

    class GenerationContext{
        int node;
        Behaviour tree = null;
        Stack<String> scope = new Stack<>();
        ArrayList<Instruction> possibilities = new ArrayList<>();
        boolean canBranch = true;
        HashMap<String, Requirement> currentExternalRequirements = new HashMap<>();
        ArrayList<String> possibleNodesMask = new ArrayList<>(IntStream.range(0, nodes).boxed().map(String::valueOf).toList());
        Requirement lastRequirement = null;
        Requirement initialRequirementTree;
        GenerationContext(int node){
            this.node = node;
            scope.add("main");
        }

        public GenerationContext reset(){
            var gc = new GenerationContext(node);
            gc.possibilities = new ArrayList<>(possibilities);
            gc.canBranch = canBranch;
            gc.possibleNodesMask = new ArrayList<>(possibleNodesMask);
            gc.scope = new Stack<>();
            gc.lastRequirement = lastRequirement;
            gc.currentExternalRequirements = new HashMap<>(currentExternalRequirements);
            return gc;
        }
    }

    @Override
    public void generateSystem() {
        ClassLoader classLoader = SPGenerator.class.getClassLoader();
        try (InputStream fis = classLoader.getResourceAsStream(rulesFile)){
            JsonReader reader = Json.createReader(fis);
            rules = reader.read().asJsonObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < nodes; i++) {
            currentCtx = new GenerationContext(i);
            currentCtx.currentExternalRequirements = requirements;
            if(requirements.containsKey(String.valueOf(i)))
                currentCtx.initialRequirementTree = requirements.get(String.valueOf(i));
            System.out.println("============= NODE "+i+" ================");
            generateNode();
            system.put(String.valueOf(i), currentCtx.tree);
        }
    }

    private void generateNode(){
        computePossibilitiesAtI(currentCtx.node);
        while((!currentCtx.possibilities.isEmpty() && !currentCtx.scope.empty()) ||
                currentCtx.currentExternalRequirements.containsKey(String.valueOf(currentCtx.node))){
            computePossibilitiesAtI(currentCtx.node);
            collapseAt(currentCtx.node);
        }
        requirements = currentCtx.currentExternalRequirements;
    }

    @Override
    public void collapseAt(int node){
        String snode = String.valueOf(node);
        if(currentCtx.currentExternalRequirements.containsKey(snode)){
            collapseRequirement(snode);
        }else{
            collapsePossibility(snode);
        }
    }

    public void collapseRequirement(String snode){
        var req = currentCtx.currentExternalRequirements.get(snode);
        if(req == null) {
            currentCtx.currentExternalRequirements.remove(snode);
            return;
        }
        currentCtx.lastRequirement = req;
        if(req.instr.getInstrName().equals("rbranch")){
            var source = ((BranchInstr)req.instr).source;
            //here
            var leftBranch = req.nextRequirements.get("left");
            var rightBranch = req.nextRequirements.get("right");
            var oldCtx = currentCtx;
            currentCtx = currentCtx.reset();
            currentCtx.currentExternalRequirements.put(snode, leftBranch);
            currentCtx.scope.push("label");
            generateNode();
            var leftCtx = currentCtx;
            currentCtx = oldCtx;
            currentCtx = currentCtx.reset();
            currentCtx.currentExternalRequirements.put(snode, rightBranch);
            currentCtx.scope.push("label");
            generateNode();
            var rightCtx = currentCtx;
            currentCtx = oldCtx;
            //create new Branch
            var hm = new HashMap<String, Behaviour>();
            hm.put("left", leftCtx.tree);
            hm.put("right", rightCtx.tree);
            var b = new Comm(String.valueOf(currentCtx.node), source, hm);
            if(currentCtx.tree == null) currentCtx.tree = b;
            else currentCtx.tree.addBehaviour(b);
            currentCtx.currentExternalRequirements.remove(snode);
        }else if(req.instr.getInstrName().equals("rif")){
            //
        }else{
            var b = req.instr.generateBehaviour(Integer.parseInt(snode), nodes);
            // can't be null since everything is determined
            if(!req.nextRequirements.isEmpty()){
                currentCtx.currentExternalRequirements.put(snode, req.nextRequirements.get(";"));
            }else{
                currentCtx.currentExternalRequirements.remove(snode);
            }
            if(req.instr instanceof SendInstr || req.instr instanceof ReceiveInstr ||
                    req.instr instanceof SelectInstr || req.instr instanceof EndInstr){
                if(currentCtx.tree == null) currentCtx.tree = b;
                else currentCtx.tree.addBehaviour(b);
            }
        }
        currentCtx.lastRequirement = req;
    }

    private void collapsePossibility(String snode){
        if(!currentCtx.possibilities.isEmpty()){
            var p = pickRandom(currentCtx.possibilities);
            var b = p.generateBehaviour(Integer.parseInt(snode), nodes);
            if(b == null) {
                b = new End(snode);
                p = new EndInstr();
            }
            //process requirements
            var p1 = evaluteSelfRules(p, Integer.parseInt(snode));
            while(p1 != null){
                b = p1.generateBehaviour(Integer.parseInt(snode), nodes);
                if(b == null) {
                    b = new End(snode);
                    p1 = new EndInstr();
                }
                p = p1;
                p1 = evaluteSelfRules(p, Integer.parseInt(snode));
            }

            evaluateNeighborRules(p,b, snode);
            if(p instanceof SendInstr || p instanceof ReceiveInstr || p instanceof SelectInstr || p instanceof EndInstr){
                if(currentCtx.tree == null) currentCtx.tree = b;
                else currentCtx.tree.addBehaviour(b);
            }
        }
    }

    private Instruction evaluteSelfRules(Instruction p, int node) {
        for (JsonValue ruleSelf : rules.getJsonObject(p.getInstrName()).getJsonArray("rule_self")) {
            switch (ruleSelf.toString().replace("\"","")){
                case "end":{
                    var s = currentCtx.scope.pop();
//                    System.out.println("exiting scope : "+s);
                    break;
                    //
                }
                case "elect-nodes":{
                    //when performing a condition, every communication will happen at most with those nodes
                    currentCtx.possibleNodesMask = new ArrayList<>(currentCtx.possibleNodesMask.stream()
                            .filter(e -> Integer.parseInt(e) > currentCtx.node).toList());
                    var toRemove = new ArrayList<String>();
                    for (int i = currentCtx.node+1; i < nodes; i++) {
                        if(Math.random()>0.70 && currentCtx.possibleNodesMask.size() > 1)
                            toRemove.add(String.valueOf(i));
                    }
                    for (String s : toRemove) {
                        currentCtx.possibleNodesMask.remove(s);
                    }
                    break;
                }
                case "switch-cdt":{
                    //generate select for every destination
                    var destinations = getPossibleNodesForI(currentCtx.node).stream()
                            .filter(item -> currentCtx.possibleNodesMask.contains(item)).toList();
                    if(destinations.isEmpty()) return new EndInstr();
//                    System.out.println("entering cdt");
                    currentCtx.scope.add("cdt");

                    var oldCtx = currentCtx;
                    currentCtx = currentCtx.reset();
                    currentCtx.scope.add("then");
                    generateSelection("left", destinations);
                    generateNode();
                    var thenCtx = currentCtx;

                    currentCtx = oldCtx;
                    currentCtx = currentCtx.reset();
                    currentCtx.scope.add("else");
                    //generate select for every destination
                    generateSelection("right", destinations);
                    generateNode();
                    var elseCtx= currentCtx;

                    var hm = new HashMap<String, Behaviour>();
                    hm.put("then", thenCtx.tree);
                    hm.put("else", elseCtx.tree);
                    var cdt = new Cdt(String.valueOf(node), hm, "myCondition");
                    currentCtx = oldCtx;
                    var s1 = currentCtx.scope.pop();
                    var s2 = currentCtx.scope.pop();
//                    System.out.println("exiting from "+s1);
//                    System.out.println("exiting from "+s2);

                    if(generateRequirementForCdt(destinations, thenCtx, elseCtx)){
                        if(currentCtx.tree != null) currentCtx.tree.addBehaviour(cdt);
                        else currentCtx.tree = cdt;
                    }
                    break;
                }
                case "switch-branch":{
                    System.out.println("entering branch");
                    var possibleNodes = getPossibleNodesForI(node);
                    int index = (int)Math.round(Math.random()*(possibleNodes.size()-1));
                    String destination = possibleNodes.get(index);
                    currentCtx.scope.add("branch");
                    ArrayList<GenerationContext> ctxs = new ArrayList<>();
                    // branching left
                    var oldCtx = currentCtx;
                    currentCtx = currentCtx.reset();
                    currentCtx.scope.add("label");
                    generateNode();
                    ctxs.add(currentCtx);
                    // branching right
                    currentCtx = oldCtx;
                    currentCtx.reset();
                    currentCtx.scope.add("label");
                    generateNode();
                    ctxs.add(currentCtx);
//                    while(Math.random()>0.3){
//                        currentCtx = oldCtx;
//                        currentCtx = currentCtx.reset();
//                        currentCtx.scope.add("label");
//                        generateNode();
//                        ctxs.add(currentCtx);
//                    }
                    currentCtx = oldCtx;
                    generateRequirementForBranch(ctxs);
                    System.out.println("exiting from "+currentCtx.scope.pop());
                    System.out.println("exiting from "+currentCtx.scope.pop());
                    HashMap<String, Behaviour> bev = new HashMap<>();
                    AtomicInteger counter = new AtomicInteger();
                    var l = ctxs.stream().peek(item -> {
                        bev.put("myLabel"+counter, item.tree);
                        counter.getAndIncrement();
                    }).toList();
                    Comm branch = new Comm(String.valueOf(node), destination, bev);
                    if(currentCtx.tree != null) currentCtx.tree.addBehaviour(branch);
                    else currentCtx.tree = branch;
                    break;
                }
                default:{
                    throw new IllegalStateException("Unexpected value: " + ruleSelf);
                }

            }
        }
        return null;
    }

    private void evaluateNeighborRules(Instruction instr, Behaviour behaviour, String snode) {
        var neighRules = rules.getJsonObject(instr.getInstrName()).getJsonArray("rule_neigh");
        for (JsonValue neighRule : neighRules) {
            switch (neighRule.toString().replace("\"","")){
                case "$comp-rrcv": {
                    var destination = ((SendInstr) instr).destination;
                    var req = new Requirement(new ReceiveInstr(snode), commCounter);
                    commCounter++;
                    insertRequirement(destination, req);
                    break;
                }
                case "$comp-rsend":{
                    var destination = ((ReceiveInstr) instr).source;
                    var req = new Requirement(new SendInstr(snode), commCounter);
                    commCounter++;
                    insertRequirement(destination, req);
                    break;
                }
                case "$comp-rbranch-rlabel-$label":{
                    break;
                }
                case "$comp-rselect-right":{
                    break;
                }
                case "$comp-rselect-left":{
                    break;
                }
                case "$comp-rselect-$label":{
                    break;
                }
                case "1":{
                    break;
                }
                default:{
                    System.err.println("weird my man : "+neighRule.toString().replace("\"",""));
                    break;
                }
            }
        }
    }

    private void insertRequirement(String destination, Requirement req) {
        if(currentCtx.lastRequirement == null && !currentCtx.currentExternalRequirements.containsKey(destination)){
            //no requirements on either side, can just put it
            currentCtx.currentExternalRequirements.put(destination, req);
        }else if(currentCtx.lastRequirement == null && currentCtx.currentExternalRequirements.containsKey(destination)){
            //current has no requirement but target has, I can either put requirement at the end of all its branch or before its first requirement
            if(Math.random() < 0.5){
                // put it at the end of every branch
                currentCtx.currentExternalRequirements.get(destination).addRequirementBroadCast(req);
            }else{
                req.addRequirementBroadCast(currentCtx.currentExternalRequirements.get(destination));
                // put it at the top of the requirements
            }
        }else if(currentCtx.lastRequirement != null && !currentCtx.currentExternalRequirements.containsKey(destination)){
            //current node has requirement but other has not, I can't put it anywhere !
            // what if I am in a branch, other process will expect a message from every branch
            var branchingChain = getBranchingRequirementChainForCurrentNode();
            if(branchingChain.isEmpty()){
                //no branching, I can put it anywhere
                currentCtx.currentExternalRequirements.put(destination, req);
            }else{
                throw new RuntimeException("no way branch");
                // branchings, I can't put it anywhere unless I add the same communication to all the branches of current
            }
        }else{
            throw new RuntimeException("no way interactions");
            // current has requirements and target has requirement
//            var reqChain = getRequirementChainForCurrentNode();
//            var destinationReqChain = getRequirementChainForNode(destination);
//            var commonChain = new ArrayList<>();
//            for (int i = 0; i < reqChain.size(); i++) {
//                if(destinationReqChain.size()<i-1) break;
//                if(reqChain.get(i).equals(destinationReqChain.get(i))) commonChain.add(reqChain.get(i));
//                else break;
//            }
        }
    }

    public boolean generateRequirementForCdt(List<String> destinations, GenerationContext thenCtx, GenerationContext elseCtx){
        for (String destination : destinations) {
            var bi = new BranchInstr(String.valueOf(currentCtx.node));
            var req = new Requirement(bi, commCounter);
            var hm = new HashMap<String, Requirement>();
            hm.put("left", thenCtx.currentExternalRequirements.get(destination));
            hm.put("right", elseCtx.currentExternalRequirements.get(destination));
            req.setRequirements(hm);
            insertRequirement(destination, req);
        }
        commCounter++;
        return true;
    }

    public void generateRequirementForBranch(ArrayList<GenerationContext> ctxs){
        for (GenerationContext ctx : ctxs) {

        }
    }

    private void generateSelection(String label, List<String> destinations){
        for (String possibleNode : destinations) {
            var select = new Comm(String.valueOf(currentCtx.node), possibleNode, Utils.Direction.SELECT, label);
            if (currentCtx.tree == null) {
                currentCtx.tree = select;
            } else {
                currentCtx.tree.addBehaviour(select);
            }
        }
    }

    private Instruction pickRandom(List<Instruction> instrs){
        var index = (int)Math.round(Math.random()*(instrs.size()-1));
        if(currentCtx.scope.peek().equals("label")){
            currentCtx.canBranch = false;
        }
        if(!currentCtx.canBranch){
            try{
                while (instrs.get(index).getInstrName().equals("rbranch")){
                    index = (int)Math.round(Math.random()*(instrs.size()-1));
                }
            }catch(Exception e){
                System.err.println(e);
            }
        }
        return instrs.get(index);
    }

    private List<Requirement> getRequirementUntilReq(Requirement tree, Requirement req){
        if(tree == null) return List.of();
        else {
            return tree.getRequirementChainUntil(req);
        }
    }

    private List<Requirement> getRequirementChainForNode(String node){
        return getRequirementUntilReq(currentCtx.currentExternalRequirements.get(node), currentCtx.lastRequirement);
    }

    private List<Requirement> getRequirementChainForCurrentNode(){
        return getRequirementUntilReq(currentCtx.initialRequirementTree, currentCtx.lastRequirement);
    }

    private List<Requirement> getBranchingRequirementChainForCurrentNode(){
        return getRequirementUntilReq(currentCtx.initialRequirementTree, currentCtx.lastRequirement)
            .stream().filter(el -> el.getInstr().getInstrName().equals("rbranch")).toList();
    }

    private List<Requirement> getBranchingRequirementChainForNode(String node){
        return getRequirementUntilReq(currentCtx.currentExternalRequirements.get(node), currentCtx.lastRequirement)
            .stream().filter(el -> el.getInstr().getInstrName().equals("rbranch")).toList();
    }

    @Override
    public void computePossibilitiesAtI(int i){
        currentCtx.possibilities = new ArrayList<>();
        if(currentCtx.scope.empty()) return;
        var possibleNodes = getPossibleNodesForI(i);
        if(possibleNodes.isEmpty()) {
            currentCtx.possibilities.add(new EndInstr());
            return;
        }
        if(currentCtx.lastRequirement != null){
            var branchingChain = getBranchingRequirementChainForCurrentNode();
            possibleNodes = possibleNodes.stream()
                    .filter(n -> Collections.indexOfSubList(
                            getBranchingRequirementChainForNode(String.valueOf(n)),
                            branchingChain) != -1)
                    .toList();
        }
        currentCtx.possibilities.addAll(getPossibleInstructionsForI(possibleNodes, i));
    }

    private List<String> getPossibleNodesForI(int i){
        return IntStream.range(i+1, nodes).boxed().map(String::valueOf)
                .filter(n -> {
                    if(currentCtx.lastRequirement == null && !currentCtx.currentExternalRequirements.containsKey(n)){
                        // no requirements on either side, can just put it
                        return true;
                    }else if(currentCtx.lastRequirement == null && currentCtx.currentExternalRequirements.containsKey(n)){
                        // current node has no requirement, but target has some
                        // I should put it on top or at the end of every branch
                        return false;
                    }else if(currentCtx.lastRequirement != null && !currentCtx.currentExternalRequirements.containsKey(n)){
                        // current node has requirement but other has not, I can't put it anywhere !
                        // what if I am in a branch, other process will expect a message from every branch
                        var branchingChain = getBranchingRequirementChainForCurrentNode();
                        if(branchingChain.isEmpty()){
                            // no branching, I can put it anywhere
                            return true;
                        }else{
                            // branchings, I can't put it anywhere unless I add the same communication to all the branches of current
                            return false;
                        }
                    }else{
                        // they both have requirements, can't
                        return false;
                    }
                })
                .filter(n -> currentCtx.possibleNodesMask.contains(n))
                .toList();
    }

    private boolean isSatisfied(String condition){
        var cdts = condition.split("\\+");
        var currentScope = currentCtx.scope.peek();
        var res = true;
        for (String cdt : cdts) {
            if(cdt.contains("scope")) {
                res = res && currentScope.equals(cdt.replace("\"", "").split("-")[1]);
            }else{
                System.err.println("weird man");
                return false;
            }
        }
        return res;
    }

    private List<Instruction> getPossibleInstructionsForI(List<String> possibleNodes, int i){
        var possibleRules = rules.entrySet().stream()
                .filter((entry) -> entry.getValue().asJsonObject().getJsonArray("cdt").stream()
                        .anyMatch(cdt -> isSatisfied(String.valueOf(cdt))))
                .map(Map.Entry::getKey)
                .toList();
        ArrayList<Instruction> pInstr = new ArrayList<>();
        for (String ruleName : possibleRules) {
            Instruction instr = switch (ruleName){
                case "rsend": yield new SendInstr(possibleNodes);
                case "rrcv": yield new ReceiveInstr(possibleNodes);    // should retrieve the one for rif
                case "rselect": yield new SelectInstr(possibleNodes);
                case "rbranch": yield new BranchInstr(possibleNodes);
                case "rif": yield new IfInstr();
                case "relse": yield new ElseInstr();
                case "rend": yield new EndInstr();
                default: {
                    System.err.println("error in rules");
                    yield null;
                }
            };
            pInstr.add(instr);
        }
        return pInstr;
    }
}