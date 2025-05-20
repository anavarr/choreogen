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
        while(possibilities.stream().anyMatch(l -> !l.isEmpty())){
            collapseAt(choseProcess());
            computePossibilities();
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
            var b = p.generateBehaviour(node, nodes);
            if (b instanceof Comm com){
                switch (com.getDirection()){
                    case SEND -> {
                        requirements.get(Integer.parseInt(com.getDestination())).
                                add(new ReceiveInstr(snode));
                    }
                    case RECEIVE -> {
                        requirements.get(Integer.parseInt(com.getDestination())).
                                add(new SendInstr(snode));
                    }
                    case SELECT -> {
                        requirements.get(Integer.parseInt(com.getDestination()))
                                .add(new BranchInstr(snode));
                    }
                    default -> {
                    }
                }
            }
            if(system.containsKey(snode)) system.get(snode).addBehaviour(b);
            else system.put(snode, b);
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
            possibilities.set(i, new ArrayList<>());
            if(!(system.containsKey(String.valueOf(i)) &&
                    (system.get(String.valueOf(i)).getLeaves().getFirst() instanceof End ||
                    system.get(String.valueOf(i)).getLeaves().getFirst() instanceof Call))){
                possibilities.set(i, new ArrayList<>());
                var possibleNodes = getPossibleNodesForI(i);
                possibilities.get(i).addAll(getPossibleInstructionsForI(possibleNodes, i));
            }
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
                case "rcall":
                    List<String> forbiddenNames = new ArrayList<>();
                    if(recursiveVariables.containsKey(String.valueOf(i)))
                        forbiddenNames = recursiveVariables.get(String.valueOf(i));
                    var ins = new CallInstr(forbiddenNames);
                    recursiveVariables.computeIfAbsent(String.valueOf(i), k -> new ArrayList<>())
                            .add(ins.getName());
                    yield ins;
                case "rend":
                    yield new EndInstr();
//            case "rselect": yield new SelectInstr();
//            case "rbranch": yield new BranchInstr();
//            case "rlabel":  yield new BranchInstr();    // should retrieve the one for rbranch
//            case "rif":     yield new CdtInstr();
//            case "relse":   yield new CdtInstr();
                default:
                    System.err.println("error in rules");
                    yield null;
            };
            pInstr.add(instr);
        }
        return pInstr;
    }

    HashMap<String, ArrayList<String>> recursiveVariables = new HashMap<>();
    ArrayList<ArrayList<Instruction>> possibilities = new ArrayList<>();
    ArrayList<ArrayList<Instruction>> requirements = new ArrayList<>();
}