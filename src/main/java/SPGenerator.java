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
        ArrayList<ArrayList<Instruction>> scopedRequirement = new ArrayList<>();
        int currentScopedRequirementIndex = 0;
        ArrayList<Instruction> requirements = new ArrayList<>();
        List<String> possibleNodesMask = IntStream.range(0, nodes).boxed().map(String::valueOf).collect(Collectors.toList());
        GenerationContext(int node){
            this.node = node;
            scope.add("main");
            scopedRequirement.add(new ArrayList<>());
        }

        public GenerationContext reset(){
            var gc = new GenerationContext(node);
            gc.possibilities = new ArrayList<>(possibilities);
            gc.canBranch = canBranch;
            gc.currentScopedRequirementIndex = currentScopedRequirementIndex;
            gc.scopedRequirement = new ArrayList<>(scopedRequirement);
            gc.possibleNodesMask = new ArrayList<>(possibleNodesMask);
            gc.requirements = new ArrayList<>(requirements);
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
        if(!currentCtx.requirements.isEmpty()) {
            var requi = currentCtx.requirements.getFirst();
            currentCtx.requirements.remove(requi);
                var b = requi.generateBehaviour(node, nodes);
            if(currentCtx.tree == null) currentCtx.tree = b;
            else currentCtx.tree.addBehaviour(b);
        }else if(!currentCtx.possibilities.isEmpty()){
            var p = pickRandom(currentCtx.possibilities);
            var b = p.generateBehaviour(node, nodes);
            if(b == null) {
                b = new End(String.valueOf(node));
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
                    currentCtx.scopedRequirement.get(currentCtx.currentScopedRequirementIndex)
                            .add(new SendInstr(snode));
//                    requirements.get(Integer.parseInt(((Comm)behaviour).getDestination())).
//                            add(new SendInstr(snode));
                    break;
                }
                case "$comp-rsend":{
                    currentCtx.scopedRequirement.get(currentCtx.currentScopedRequirementIndex)
                            .add(new ReceiveInstr(snode));
//                    requirements.get(Integer.parseInt(((Comm)behaviour).getDestination())).
//                            add(new ReceiveInstr(snode));
                    break;
                }
                case "$comp-rbranch-rlabel-$label":{
                    var branch = new BranchInstr(snode);
                    currentCtx.scopedRequirement.get(currentCtx.currentScopedRequirementIndex)
                            .add(new ReceiveInstr(snode));
//                    requirements.get(Integer.parseInt(((Comm)behaviour).getDestination()))
//                            .addAll(List.of(branch, new LabelInstr(com.labels.getFirst(), branch)));
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
                    currentCtx.currentScopedRequirementIndex ++;
                    currentCtx.scopedRequirement.add(new ArrayList<>());
                    var oldCtx = currentCtx;
                    currentCtx = currentCtx.reset();
                    currentCtx.scope.add("then");
                    generateNode();
                    var thenCtx = currentCtx;
                    currentCtx = oldCtx;
                    currentCtx = currentCtx.reset();
                    currentCtx.scope.add("else");
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
                    currentCtx.currentScopedRequirementIndex ++;
                    currentCtx.scopedRequirement.add(new ArrayList<>());
                    ArrayList<GenerationContext> ctxs = new ArrayList<>();
                    var oldCtx = currentCtx;
                    currentCtx = currentCtx.reset();
                    currentCtx.scope.add("label");
                    generateNode();
                    ctxs.add(currentCtx);
                    while(Math.random()>0.3){
                        currentCtx = oldCtx;
                        currentCtx = currentCtx.reset();
                        currentCtx.scope.add("label");
                        generateNode();
                        ctxs.add(currentCtx);
                    }
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

    private Instruction pickRandom(List<Instruction> instrs){
        var index = (int)Math.round(Math.random()*(instrs.size()-1));
        if(currentCtx.scope.peek().equals("label")){
            currentCtx.canBranch = false;
        }
        if(!currentCtx.canBranch){
            while (instrs.get(index).getInstrName().equals("rbranch")){
                index = (int)Math.round(Math.random()*(instrs.size()-1));
            }
        }
        return instrs.get(index);
    }

    public int choseProcess(){
        int pr = (int)Math.round(Math.random()*(nodes-1));
        while(possibilities.get(pr).isEmpty()){
            pr = (int)Math.round(Math.random()*(nodes-1));
        }
        return pr;
    }

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
        var possibleNodes = new ArrayList<String>();
        for (int i1 = 0; i1 < nodes; i1++) {
            if(i1 != i && !(system.containsKey(String.valueOf(i1)) &&
                    (system.get(String.valueOf(i1)).getLeaves().getFirst() instanceof End))){
                possibleNodes.add(String.valueOf(i1));
            }
        }
        return possibleNodes;
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
//                case "rcall":
//                    List<String> forbiddenNames = new ArrayList<>();
//                    if(recursiveVariables.containsKey(String.valueOf(i)))
//                        forbiddenNames = recursiveVariables.get(String.valueOf(i));
//                    var ins = new CallInstr(forbiddenNames);
//                    recursiveVariables.computeIfAbsent(String.valueOf(i), k -> new ArrayList<>())
//                            .add(ins.getName());
//                    yield ins;
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

    ArrayList<ArrayList<ArrayList<Instruction>>> scopedRequirements = new ArrayList<>();
    ArrayList<ArrayList<Instruction>> possibilities = new ArrayList<>();
    ArrayList<ArrayList<Instruction>> requirements = new ArrayList<>();
}