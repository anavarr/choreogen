import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Behaviour.Behaviour;
import jakarta.json.*;
import Behaviour.End;
import Behaviour.Comm;
import Behaviour.Call;
import Behaviour.Cdt;

public class SPGenerator implements Generator{
    int nodes;
    JsonObject rules;
    String rulesFile = "rules_valid.json";

    HashMap<String, Behaviour> system = new HashMap<String, Behaviour>();

    SPGenerator(int nodes){
        this.nodes = nodes;
        for (int i = 0; i < nodes; i++) {
            possibilities.add(new ArrayList<>());
            requirements.add(new ArrayList<>());
        }
    }


    SPGenerator(int nodes, String rulesFile){
        this(nodes);
        this.rulesFile = rulesFile;

    }

    @Override
    public void generateSystem() {
        for (int i = 0; i < nodes; i++) {
            while(!possibilities.get(i).isEmpty()){
                collapseAt(i);
                computePossibilitiesAtI(i);
            }
        }
    }

    @Override
    public void collapseAt(int node){
        String snode = String.valueOf(node);
        if(!requirements.get(node).isEmpty()) {
            var requi = requirements.get(node).getFirst();
            requirements.get(node).remove(requi);
                var b = requi.generateBehaviour(node, nodes);
            if(system.containsKey(snode)) system.get(snode).addBehaviour(b);
            else system.put(snode, b);
        }else if(!possibilities.get(node).isEmpty()){
            var p = pickRandom(possibilities.get(node));
            //process requirements
            evaluteSelfRules(p);
            var b = p.generateBehaviour(node, nodes);
            evaluateNeighborRules(p,b, snode);
            if(system.containsKey(snode)) system.get(snode).addBehaviour(b);
            else system.put(snode, b);
        }
    }

    private void evaluateNeighborRules(Instruction instr, Behaviour behaviour, String snode) {
        var neighRules = rules.getJsonObject(instr.getInstrName()).getJsonArray("rules_neigh");
        for (JsonValue neighRule : neighRules) {
            switch (neighRules.toString()){
                case "$comp-receive": {
                    requirements.get(Integer.parseInt(((Comm)behaviour).getDestination())).
                            add(new SendInstr(snode));
                }
                case "$comp-send":{
                    requirements.get(Integer.parseInt(((Comm)behaviour).getDestination())).
                            add(new ReceiveInstr(snode));
                }
                case "$comp-branch-$label":{
                    var branch = new BranchInstr(snode);
                    requirements.get(Integer.parseInt(((Comm)behaviour).getDestination()))
                            .addAll(List.of(branch, new LabelInstr(com.labels.getFirst(), branch)));
                }
                case "$comp-select-$label":{

                }
                default:{
                    System.err.println("weird my man");
                }
            }
        }
        switch (behaviour){
            case Comm com -> {
                switch (com.getDirection()){
                    case SEND -> {

                    }
                    case RECEIVE -> {
                    }
                    case SELECT -> {
                    }
                    case BRANCH -> {
                        latestBranch = com;
//                            requirements.get(Integer.parseInt(com.getDestination()))
//                                    .add(new SelectInstr(snode, label));
                    }
                    default -> {
                    }
                }
            }
            case Cdt cdt -> {

            }
            case Call call -> {

            }
            case End end -> {

            }
            default -> throw new IllegalStateException("Unexpected value: " + behaviour);
        }
    }


    private void evaluteSelfRules(Instruction p) {
        for (JsonValue ruleSelf : rules.getJsonObject(p.getInstrName()).getJsonArray("rule_self")) {
            if (ruleSelf.toString().equals("must-elect-label")) {
                // do stuff
            } else if(ruleSelf.toString().equals("b")) {

            }else{
                throw new IllegalStateException("Unexpected value: " + ruleSelf);
            }
        }
    }

    private Instruction pickRandom(List<Instruction> instrs){
        var index = (int)Math.round(Math.random()*(instrs.size()-1));
        return instrs.get(index);
    }

    public void computeInitialPossibilities() {
        ClassLoader classLoader = SPGenerator.class.getClassLoader();
        try (InputStream fis = classLoader.getResourceAsStream(rulesFile)){
            JsonReader reader = Json.createReader(fis);
            var nnames=new ArrayList<String>();
            for (int i = 0; i < nodes; i++) {
                nnames.add(String.valueOf(i));
            }
            rules = reader.read().asJsonObject();
            var l = rules.entrySet().stream()
                    .filter(entry -> entry.getValue().asJsonObject().getJsonArray("cdt").isEmpty()).map(Map.Entry::getKey).toList();
            for (int i = 0; i < nodes; i++) {
                for (String s : rules.keySet()) {
                    if(rules.getJsonObject(s).getJsonArray("cdt").isEmpty()) {
                        try {
                            possibilities.get(i).add(Instruction.getIntrForRule(s, String.valueOf(i), nnames));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int choseProcess(){
        int pr = (int)Math.round(Math.random()*(nodes-1));
        while(possibilities.get(pr).isEmpty()){
            pr = (int)Math.round(Math.random()*(nodes-1));
        }
        return pr;
    }

    @Override
    public void computePossibilities(){
        for (int i = 0; i < nodes; i++) {
            computePossibilitiesAtI(i);
        }
    }

    public void computePossibilitiesAtI(int i){
        possibilities.set(i, new ArrayList<>());
        if(!(system.containsKey(String.valueOf(i)) &&
                (system.get(String.valueOf(i)).getLeaves().getFirst() instanceof End ||
                        system.get(String.valueOf(i)).getLeaves().getFirst() instanceof Call))){
            possibilities.set(i, new ArrayList<>());
            var possibleNodes = getPossibleNodesForI(i);
            possibilities.get(i).addAll(getPossibleInstructionsForI(possibleNodes, i));
        }
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

    private List<Instruction> getPossibleInstructionsForI(List<String> possibleNodes, int i){
        var possibleRules = rules.entrySet().stream()
                .filter((entry) -> entry.getValue().asJsonObject().getJsonArray("cdt").isEmpty())
                .map(Map.Entry::getKey)
                .toList();
        ArrayList<Instruction> pInstr = new ArrayList<>();
        for (String ruleName : possibleRules) {
            Instruction instr = switch (ruleName){
                case "rsend":
                    yield new SendInstr(possibleNodes);
                case "rrcv":
                    yield new ReceiveInstr(possibleNodes);    // should retrieve the one for rif
//                case "rcall":
//                    List<String> forbiddenNames = new ArrayList<>();
//                    if(recursiveVariables.containsKey(String.valueOf(i)))
//                        forbiddenNames = recursiveVariables.get(String.valueOf(i));
//                    var ins = new CallInstr(forbiddenNames);
//                    recursiveVariables.computeIfAbsent(String.valueOf(i), k -> new ArrayList<>())
//                            .add(ins.getName());
//                    yield ins;
                case "rend":
                    yield new EndInstr();
//            case "rselect": yield new SelectInstr();
//            case "rbranch": yield new BranchInstr();
//            case "rlabel":  yield new BranchInstr();    // should retrieve the one for rbranch
                default:
                    System.err.println("error in rules");
                    yield null;
            };
            pInstr.add(instr);
        }
        return pInstr;
    }
    Comm latestBranch;
    ArrayList<Requirement> scopedRequirements = new ArrayList<>();
    HashMap<String, ArrayList<String>> recursiveVariables = new HashMap<>();
    ArrayList<ArrayList<Instruction>> possibilities = new ArrayList<>();
    ArrayList<ArrayList<Instruction>> requirements = new ArrayList<>();
}