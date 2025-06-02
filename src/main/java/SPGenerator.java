import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
            var b = new Comm(source, String.valueOf(currentCtx.node), hm);
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
            evaluteSelfRules(p, Integer.parseInt(snode));
            evaluateNeighborRules(p,b, snode);
            if(p instanceof SendInstr || p instanceof ReceiveInstr || p instanceof SelectInstr || p instanceof EndInstr){
                if(currentCtx.tree == null) currentCtx.tree = b;
                else currentCtx.tree.addBehaviour(b);
            }
        }
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

    private void evaluateNeighborRules(Instruction instr, Behaviour behaviour, String snode) {
        var neighRules = rules.getJsonObject(instr.getInstrName()).getJsonArray("rule_neigh");
        for (JsonValue neighRule : neighRules) {
            switch (neighRule.toString().replace("\"","")){
                case "$comp-rrcv": {
                    var destination = ((Comm) behaviour).getDestination();
                    var req = new Requirement(new ReceiveInstr(snode), commCounter);
                    commCounter++;
                    if(currentCtx.currentExternalRequirements.containsKey(destination))
                        currentCtx.currentExternalRequirements.get(destination).addRequirement(req);
                    else
                        currentCtx.currentExternalRequirements.put(destination, req);
                    break;
                }
                case "$comp-rsend":{
                    var destination = ((Comm) behaviour).getDestination();
                    var req = new Requirement(new SendInstr(snode), commCounter);
                    commCounter++;
                    if(currentCtx.currentExternalRequirements.containsKey(destination))
                        currentCtx.currentExternalRequirements.get(destination).addRequirement(req);
                    else
                        currentCtx.currentExternalRequirements.put(destination, req);
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

    private void evaluteSelfRules(Instruction p, int node) {
        for (JsonValue ruleSelf : rules.getJsonObject(p.getInstrName()).getJsonArray("rule_self")) {
            switch (ruleSelf.toString().replace("\"","")){
                case "end":{
                    System.out.println("exiting scope : "+currentCtx.scope.pop());
                    break;
                    //
                }
                case "elect-nodes":{
                    //when performing a condition, every communication will happen at most with those nodes
                    currentCtx.possibleNodesMask = new ArrayList<>(currentCtx.possibleNodesMask.stream()
                            .filter(e -> Integer.parseInt(e) > currentCtx.node).toList());
                    var toRemove = new ArrayList<String>();
                    for (int i = currentCtx.node+1; i < nodes; i++) {
                        if(Math.random()>=0.50 && currentCtx.possibleNodesMask.size() > 1)
                            toRemove.add(String.valueOf(i));
                    }
                    for (String s : toRemove) {
                        currentCtx.possibleNodesMask.remove(s);
                    }
                    break;
                }
                case "switch-cdt":{
                    System.out.println("entering cdt");
                    currentCtx.scope.add("cdt");

                    var oldCtx = currentCtx;
                    currentCtx = currentCtx.reset();
                    currentCtx.scope.add("then");
                    //generate select for every destination
                    var oldPossibleMasks = currentCtx.possibleNodesMask;
                    var destinations = generateSelection("left");
                    while(destinations.isEmpty() && !getPossibleNodesForI(currentCtx.node).isEmpty()){
                        currentCtx.possibleNodesMask = oldPossibleMasks;
                        destinations = generateSelection("left");
                    }
                    generateSelection("left", destinations);
                    var possibleNodesMask = new ArrayList<>(currentCtx.possibleNodesMask);
                    generateNode();
                    var thenCtx = currentCtx;

                    currentCtx = oldCtx;
                    currentCtx = currentCtx.reset();
                    currentCtx.possibleNodesMask = possibleNodesMask;
                    currentCtx.scope.add("else");
                    //generate select for every destination
                    generateSelection("right", destinations);
                    generateNode();
                    var elseCtx= currentCtx;

                    var hm = new HashMap<String, Behaviour>();
                    hm.put("then", thenCtx.tree);
                    hm.put("else", elseCtx.tree);
                    var cdt = new Cdt(String.valueOf(node), hm, "check X");
                    currentCtx = oldCtx;
                    System.out.println("exiting from "+currentCtx.scope.pop());
                    System.out.println("exiting from "+currentCtx.scope.pop());
                    if(currentCtx.tree != null) currentCtx.tree.addBehaviour(cdt);
                    else currentCtx.tree = cdt;
                    generateRequirementForCdt(destinations, thenCtx, elseCtx);
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
    }

    public void generateRequirementForCdt(List<String> destinations, GenerationContext thenCtx, GenerationContext elseCtx){
        for (String destination : destinations) {
            var bi = new BranchInstr(String.valueOf(currentCtx.node));
            var req = new Requirement(bi, commCounter);
            var hm = new HashMap<String, Requirement>();
            hm.put("left", thenCtx.currentExternalRequirements.get(destination));
            hm.put("right", elseCtx.currentExternalRequirements.get(destination));
            req.setRequirements(hm);
            if(currentCtx.currentExternalRequirements.containsKey(destination)){
                currentCtx.currentExternalRequirements.get(destination).addRequirement(req);
            }else{
                currentCtx.currentExternalRequirements.put(destination, req);
            }
        }
        commCounter++;
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

    private List<String> generateSelection(String label){
        var possibleNodes = getPossibleNodesForI(currentCtx.node);
        currentCtx.possibleNodesMask = new ArrayList<>(
                currentCtx.possibleNodesMask.stream().filter(el -> Math.random() > 0.7).toList());
        return possibleNodes.stream()
                .filter(item -> currentCtx.possibleNodesMask.contains(item)).toList();
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

    private List<Requirement> getRequirementChainForNode(String node){
        var req = currentCtx.currentExternalRequirements.get(node);
        if(req == null) return List.of();
        else return req.getRequirementChainUntil(currentCtx.lastRequirement);
    }

    private List<Requirement> getRequirementChainForCurrentNode(){
        var req = currentCtx.initialRequirementTree;
        if(req == null) return List.of();
        else return req.getRequirementChainUntil(currentCtx.lastRequirement);
    }

    @Override
    public void computePossibilitiesAtI(int i){
        currentCtx.possibilities = new ArrayList<>();
        if(currentCtx.scope.empty()) return;
        var possibleNodes = IntStream.range(i+1, nodes).boxed()
                .map(String::valueOf)
                .filter(n -> currentCtx.possibleNodesMask.contains(n))
                .toList();
        if(currentCtx.lastRequirement != null){
            var chain = getRequirementChainForCurrentNode();
            possibleNodes = possibleNodes.stream()
                    .filter(n -> getRequirementChainForNode(String.valueOf(n)).containsAll(chain)).toList();
        }
//        if(currentCtx.lastRequirementId != -1){
//            possibleNodes = possibleNodes.stream()
//                    .filter(p ->
//                            currentCtx.currentExternalRequirements.get(p)
//                            .hasRequirementId(currentCtx.lastRequirementId)).toList();
//        }
        currentCtx.possibilities.addAll(getPossibleInstructionsForI(possibleNodes, i));
    }

    private List<String> getPossibleNodesForI(int i){
        return IntStream.range(i+1, nodes).boxed().map(n -> String.valueOf(n)).toList();
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