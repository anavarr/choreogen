import Behaviour.Behaviour;
import Behaviour.End;

import java.util.List;

public class EndInstr implements Instruction{
    @Override
    public List<String> getPossiblesNodes() {
        return List.of();
    }

    @Override
    public Boolean removePossibleNode(String node) {
        return true;
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        return new End(String.valueOf(node));
    }
}
