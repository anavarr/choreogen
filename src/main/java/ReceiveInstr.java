import Behaviour.Behaviour;
import Behaviour.Utils;
import Behaviour.Comm;
import Behaviour.End;

import java.util.ArrayList;
import java.util.List;


public class ReceiveInstr extends CommInstr implements Instruction{
    String instrName = "rrcv";
    String source;

    public ReceiveInstr(){

    }

    public ReceiveInstr(String source){
        this.source = source;
    }

    public ReceiveInstr(List<String> possibleSources){
        this.possibleNodes = possibleSources;
    }

    @Override
    public String getInstrName() {
        return instrName;
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        if(source == null) {
            if(possibleNodes.isEmpty()) return null;
            source = randomPick();
        }
        return new Comm(String.valueOf(node),source, Utils.Direction.RECEIVE, "");
    }
}
