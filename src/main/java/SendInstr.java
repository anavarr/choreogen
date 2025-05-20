import Behaviour.Behaviour;
import Behaviour.Comm;
import Behaviour.End;
import Behaviour.Utils;

import java.util.ArrayList;
import java.util.List;

public class SendInstr implements Instruction{
    String instrName = "rsend";
    String destination;

    List<String> possibleDestinations = new ArrayList<>();

    public SendInstr(List<String> possibleDestinations){
        this.possibleDestinations = possibleDestinations;
    }

    public SendInstr(){

    }

    public SendInstr(String destination){
        this.destination = destination;
    }

    public Behaviour generateBehaviour(int node, int range){
        if(destination == null){
            if(possibleDestinations.isEmpty()) return new End(String.valueOf(node));
            destination = randomPick();
        }
        return new Comm(String.valueOf(node), destination, Utils.Direction.SEND, "");
    }

    @Override
    public List<String> getPossiblesNodes() {
        return List.of();
    }

    @Override
    public Boolean removePossibleNode(String node) {
        return possibleDestinations.remove(node);
    }

    @Override
    public String getInstrName() {
        return instrName;
    }

    public String randomPick(){
        int index = (int)Math.round(Math.random()*(possibleDestinations.size()-1));
        return possibleDestinations.get(index);
    }
}
