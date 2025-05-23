import Behaviour.Behaviour;
import Behaviour.Comm;
import Behaviour.End;
import Behaviour.Utils;

import java.util.HashMap;
import java.util.List;

public class BranchInstr extends CommInstr implements Instruction{
    String instrName = "rbranch";
    String source;

    public BranchInstr(String source){
        this.source = source;
    }

    public BranchInstr(List<String> possibleNodes){
        this.possibleNodes = possibleNodes;
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
        return new Comm(String.valueOf(node), source, new HashMap<>());
    }
}
