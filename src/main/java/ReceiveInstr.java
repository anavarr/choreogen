import Behaviour.Behaviour;
import Behaviour.Utils;
import Behaviour.Comm;
import Behaviour.End;

import java.util.ArrayList;
import java.util.List;


public class ReceiveInstr implements Instruction{
    String source;
    List<String> possibleSources = new ArrayList<>();

    public ReceiveInstr(){

    }

    public ReceiveInstr(String source){
        this.source = source;
    }

    public ReceiveInstr(List<String> possibleSources){
        this.possibleSources = possibleSources;
    }

    @Override
    public List<String> getPossiblesNodes() {
        return possibleSources;
    }

    @Override
    public Boolean removePossibleNode(String node) {
        return possibleSources.remove(node);
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        if(source == null) {
            if(possibleSources.isEmpty()) return new End(String.valueOf(node));
            source = randomPick();
        }
        return new Comm(String.valueOf(node),source, Utils.Direction.RECEIVE, "");
    }

    public String randomPick(){
        int index = (int)Math.round(Math.random()*(possibleSources.size()-1));
        return possibleSources.get(index);
    }
}
