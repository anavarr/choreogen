import Behaviour.Behaviour;
import Behaviour.Comm;
import Behaviour.End;
import Behaviour.Utils;

import java.util.ArrayList;
import java.util.List;

public class SendInstr extends  CommInstr implements Instruction{
    String instrName = "rsend";
    String destination;


    public SendInstr(List<String> possibleDestinations){
        this.possibleNodes = possibleDestinations;
    }

    public SendInstr(){

    }

    public SendInstr(String destination){
        this.destination = destination;
    }

    public Behaviour generateBehaviour(int node, int range){
        if(destination == null){
            if(possibleNodes.isEmpty()) return null;
            destination = randomPick();
        }
        return new Comm(String.valueOf(node), destination, Utils.Direction.SEND, "");
    }

    @Override
    public String getInstrName() {
        return instrName;
    }
}
