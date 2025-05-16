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

import static Behaviour.Utils.Direction.RECEIVE;
import static Behaviour.Utils.Direction.SEND;

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
    public void generateReceive(String source, String variable) {

    }
    @Override
    public void generateBranching(String source, String label) {

    }
    @Override
    public void generateBranch(String label) {

    }
    @Override
    public void generateSend(String destination, String variable) {

    }
    @Override
    public void generateSelect(String destination, String label) {

    }
    @Override
    public void generateIf(String cdt) {

    }
    @Override
    public void generateElse() {

    }
    @Override
    public void generateCall(String variableName) {

    }
    @Override
    public void generateDef(String variableName) {

    }
    @Override
    public void generateEnd() {

    }
    @Override
    public void generateSystem() {
        while(possibilities.stream().anyMatch(l -> !l.isEmpty())){
            collapseAt(choseProcess());
            computePossibilities();
        }
    }
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

    public void computePossibilities(){
        for (int i = 0; i < nodes; i++) {
            if(system.containsKey(String.valueOf(i))){
                var leaf = system.get(String.valueOf(i)).getLeaves().getFirst();
                if(leaf instanceof End) {
                    possibilities.set(i, new ArrayList<>());
                }else{
                    possibilities.set(i, new ArrayList<>());
                    var possibleNodes = new ArrayList<String>();
                    for (int i1 = 0; i1 < nodes; i1++) {
                        if(i1 != i && (!system.containsKey(String.valueOf(i1)) || !(system.get(String.valueOf(i1))
                                .getLeaves().getFirst() instanceof End))){
                            possibleNodes.add(String.valueOf(i1));
                        }
                    }
                    possibilities.get(i).add(new SendInstr(possibleNodes));
                    possibilities.get(i).add(new ReceiveInstr(possibleNodes));
                    possibilities.get(i).add(new EndInstr());
                }
            }else{
                possibilities.set(i, new ArrayList<>());
                var possibleNodes = new ArrayList<String>();
                for (int i1 = 0; i1 < nodes; i1++) {
                    if(i1 != i && (!system.containsKey(String.valueOf(i1)) || !(system.get(String.valueOf(i1))
                            .getLeaves().getFirst() instanceof End))){
                        possibleNodes.add(String.valueOf(i1));
                    }
                }
                possibilities.get(i).add(new SendInstr(possibleNodes));
                possibilities.get(i).add(new ReceiveInstr(possibleNodes));
                possibilities.get(i).add(new EndInstr());
            }

        }
    }

    ArrayList<ArrayList<Instruction>> possibilities = new ArrayList<>();

    ArrayList<ArrayList<Instruction>> requirements = new ArrayList<>();
}