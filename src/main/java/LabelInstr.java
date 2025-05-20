import Behaviour.Behaviour;

import java.util.List;

public class LabelInstr implements Instruction{
    String label;

    public LabelInstr(String label){
        this.label = label;
    }


    @Override
    public List<String> getPossiblesNodes() {
        return List.of();
    }

    @Override
    public Boolean removePossibleNode(String node) {
        return null;
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        return null;
    }
}
