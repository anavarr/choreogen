import Behaviour.Behaviour;
import Behaviour.Call;

import java.util.List;

public class DefInstr implements Instruction{
    String instrName = "rdef";
    Call callPoint;
    List<String> possibleVarNames;
    String varName;

    public DefInstr(List<String> possibleVarNames){
        this.possibleVarNames = possibleVarNames;
    }

    public DefInstr(String varName){
        this.varName  = varName;
    }

    @Override
    public List<String> getPossiblesNodes() {
        return List.of();
    }

    @Override
    public Boolean removePossibleNode(String node) {
        return null;
    }

    @Override
    public String getInstrName() {
        return instrName;
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        if(varName != null) return new Call(String.valueOf(node), varName);
        else return new Call(String.valueOf(node), pickRandom());
    }

    private String pickRandom(){
        if(possibleVarNames == null) return null;
        return possibleVarNames.get((int)Math.round(Math.random()*(possibleVarNames.size()-1)));
    }



}
