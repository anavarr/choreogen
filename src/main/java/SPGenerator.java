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

    HashMap<String, Behaviour> system = new HashMap<>();

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
        List<String> possibleNodesMask = IntStream.range(0, nodes).boxed().map(String::valueOf).collect(Collectors.toList());
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
            return gc;
        }
    }

    GenerationContext currentCtx;

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
            System.out.println("============= NODE "+i+" ================");
            generateNode();
            system.put(String.valueOf(i), currentCtx.tree);
        }
    }

    private void generateNode(){
        computePossibilitiesAtI(currentCtx.node);
        while(!currentCtx.possibilities.isEmpty() && !currentCtx.scope.empty()){
            computePossibilitiesAtI(currentCtx.node);
            collapseAt(currentCtx.node);
        }
    }


    @Override
    public void collapseAt(int node){
        String snode = String.valueOf(node);
//        if(!currentCtx.currentRequirement == null) {
//            var requi = currentCtx.requirements.getFirst();
//            currentCtx.requirements.remove(requi);
//                var b = requi.generateBehaviour(node, nodes);
//            if(currentCtx.tree == null) currentCtx.tree = b;
//            else currentCtx.tree.addBehaviour(b);
//        }else
        if(!currentCtx.possibilities.isEmpty()){
            var p = pickRandom(currentCtx.possibilities);
            var b = p.generateBehaviour(node, nodes);
            if(b == null) {
                b = new End(snode);
                p = new EndInstr();
            }
            //process requirements
            evaluteSelfRules(p, node);
            evaluateNeighborRules(p,b, snode);
            if(p instanceof SendInstr || p instanceof ReceiveInstr || p instanceof SelectInstr || p instanceof EndInstr){
                if(currentCtx.tree == null) currentCtx.tree = b;
                else currentCtx.tree.addBehaviour(b);
            }
        }
    }

    private void evaluateNeighborRules(Instruction instr, Behaviour behaviour, String snode) {
        var neighRules = rules.getJsonObject(instr.getInstrName()).getJsonArray("rule_neigh");
        for (JsonValue neighRule : neighRules) {
            switch (neighRule.toString().replace("\"","")){
                case "$comp-rrcv": {
                    var destination = ((Comm) behaviour).getDestination();
                    var req = new Requirement(new ReceiveInstr(snode));
                    if(currentCtx.currentExternalRequirements.containsKey(destination))
                        currentCtx.currentExternalRequirements.get(destination).addRequirement(req);
                    else
                        currentCtx.currentExternalRequirements.computeIfAbsent(((Comm) behaviour).getDestination(),
                            k -> new Requirement(new ReceiveInstr(snode)));
                    break;
                }
                case "$comp-rsend":{
                    var destination = ((Comm) behaviour).getDestination();
                    var req = new Requirement(new SendInstr(snode));
                    if(currentCtx.currentExternalRequirements.containsKey(destination))
                        currentCtx.currentExternalRequirements.get(destination).addRequirement(req);
                    else
                        currentCtx.currentExternalRequirements.computeIfAbsent(((Comm) behaviour).getDestination(),
                                k -> new Requirement(new SendInstr(snode)));
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
                    for (int i = 0; i < nodes; i++) {
                        if(Math.random()>=0.50 && currentCtx.possibleNodesMask.size() > 1)
                            currentCtx.possibleNodesMask.remove(String.valueOf(i));
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
                    generateSelection("left");
                    generateNode();
                    var thenCtx = currentCtx;
                    currentCtx = oldCtx;
                    currentCtx = currentCtx.reset();
                    currentCtx.scope.add("else");
                    //generate select for every destination
                    generateSelection("right");
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
                    System.out.println("exiting from "+currentCtx.scope.pop());
                    System.out.println("exiting from "+currentCtx.scope.pop());
                    HashMap<String, Behaviour> bev = new HashMap<String, Behaviour>();
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

    private void generateSelection(String label){
        var possibleNodes = getPossibleNodesForI(currentCtx.node);
        possibleNodes = possibleNodes.stream()
                .filter(item -> currentCtx.possibleNodesMask.contains(item))
                .filter(el -> Math.random() > 0.7).toList();
        for (String possibleNode : possibleNodes) {
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

//    public int choseProcess(){
//        int pr = (int)Math.round(Math.random()*(nodes-1));
//        while(possibilities.get(pr).isEmpty()){
//            pr = (int)Math.round(Math.random()*(nodes-1));
//        }
//        return pr;
//    }

    @Override
    public void computePossibilitiesAtI(int i){
        currentCtx.possibilities = new ArrayList<>();
        if(currentCtx.scope.empty()) return;
        var possibleNodes = IntStream.range(i+1, nodes).boxed()
                .map(String::valueOf)
                .filter(n -> currentCtx.possibleNodesMask.contains(n))
                .toList();
        currentCtx.possibilities.addAll(getPossibleInstructionsForI(possibleNodes, i));
    }

    private List<String> getPossibleNodesForI(int i){
//        var possibleNodes = new ArrayList<String>();
//        for (int i1 = 0; i1 < nodes; i1++) {
//            if(i1 != i && !(system.containsKey(String.valueOf(i1)) &&
//                    (system.get(String.valueOf(i1)).getLeaves().getFirst() instanceof End))){
//                possibleNodes.add(String.valueOf(i1));
//            }
//        }
        return IntStream.range(i, nodes).boxed().map(n -> String.valueOf(n)).toList();
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