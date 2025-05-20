import Behaviour.Behaviour;
import Behaviour.Comm;
import Behaviour.End;
import Behaviour.Utils;

import java.util.HashMap;
import java.util.List;

public class BranchInstr extends CommInstr implements Instruction{
    String instrName = "rbranch";

    @Override
    public String getInstrName() {
        return instrName;
    }

    String source;
    List<String> possibleNodes;

    public BranchInstr(String source){
        this.source = source;
    }

    public BranchInstr(List<String> possibleNodes){
        this.possibleNodes = possibleNodes;
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        if(source == null) {
            if(possibleNodes.isEmpty()) return new End(String.valueOf(node));
            source = randomPick();
        }
        return new Comm(String.valueOf(node), source, new HashMap<>());
    }
}
