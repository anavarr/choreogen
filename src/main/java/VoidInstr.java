import Behaviour.Behaviour;

import java.util.List;

public class VoidInstr implements Instruction{
    String instrName = "rvoid";
    @Override
    public List<String> getPossiblesNodes() {
        return List.of();
    }

    @Override
    public Boolean removePossibleNode(String node) {
        return false;
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
