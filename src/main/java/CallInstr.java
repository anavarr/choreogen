import Behaviour.Behaviour;
import Behaviour.Call;

import java.util.ArrayList;
import java.util.List;

public class CallInstr implements Instruction{

    ArrayList<String> forbiddenNames = new ArrayList<>();
    String name;

    @Override
    public List<String> getPossiblesNodes() {
        return List.of();
    }

    public CallInstr(List<String> forbiddenNames){
        this.forbiddenNames.addAll(forbiddenNames);
        this.name = generateName();
    }

    public String getName(){
        return this.name;
    }

    @Override
    public Boolean removePossibleNode(String node) {
        return null;
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        return new Call(String.valueOf(node), name);
    }

    private String generateName(){
        int counter = 0;
        var prefix = "myVariable";
        var name = prefix;
        while(forbiddenNames.contains(name)){
            name = prefix+counter;
            counter++;
        }
        return name;
    }
}
