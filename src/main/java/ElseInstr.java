import Behaviour.Behaviour;

import java.util.List;

public class ElseInstr implements Instruction{
    String instrName = "relse";
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
        return null;
    }
}
