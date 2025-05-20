import Behaviour.Behaviour;
import Behaviour.Cdt;

import java.util.List;

public class IfInstr implements Instruction{
    String instrName = "rif";
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
        return new Cdt(String.valueOf(node), "check(expr)");
    }
}
