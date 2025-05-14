import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.*;
public class SPGenerator implements Generator{
    int nodes;
    JsonObject rules;

    SPGenerator(int nodes){
        this.nodes = nodes;
        for (int i = 0; i < nodes; i++) {
            possibilities.add(new ArrayList<>());
            requirements.add(new ArrayList<>());
        }
        this.computeInitialPossibilities();
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
    public void generateSystem(int nodes) {
//        while(!possibilities.isEmpty()){
//            collapseAt(choseProcess());
//        }
    }
    public void collapseAt(int node){
        if(!requirements.get(node).isEmpty()){
            var requi = requirements.get(node).getFirst();
            System.out.println(requi);
        }
    }
    public void computeInitialPossibilities() {
        ClassLoader classLoader = SPGenerator.class.getClassLoader();
        try (InputStream fis = classLoader.getResourceAsStream("rules_valid.json")){
            JsonReader reader = Json.createReader(fis);
            rules = reader.read().asJsonObject();
            var l = rules.entrySet().stream()
                    .filter(entry -> entry.getValue().asJsonObject().getJsonArray("cdt").isEmpty()).map(Map.Entry::getKey).toList();
            for (int i = 0; i < nodes; i++) {
                for (String s : rules.keySet()) {
                    if(rules.getJsonObject(s).getJsonArray("cdt").isEmpty()) {
                        try {
                            possibilities.get(i).add(Instruction.getIntrForRule(s));
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

    ArrayList<ArrayList<Instruction>> possibilities = new ArrayList<>();

    ArrayList<ArrayList<Instruction>> requirements = new ArrayList<>();
}