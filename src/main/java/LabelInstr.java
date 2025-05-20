import Behaviour.Behaviour;

import java.util.List;

public class LabelInstr implements Instruction{
    String instrName = "rlabel";
    String label;
    BranchInstr branch;

    public LabelInstr(String label, BranchInstr branch){
        this.label = label;
        this.branch = branch;
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
    public String getInstrName() {
        return instrName;
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        return null;
    }
}
