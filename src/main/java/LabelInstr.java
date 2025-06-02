import Behaviour.Behaviour;
import Behaviour.Comm;
import java.util.List;

public class LabelInstr implements Instruction{
    String instrName = "rlabel";
    Comm branch;
    String label;

    public LabelInstr(Comm branch){
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

    private String generateLabel(){
        int counter = 0;
        String l = "myLabel"+counter;
        while(branch.nextBehaviours.keySet().contains(l)) l = "myLabel"+counter++;
        return l;
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        if(label == null) label = generateLabel();
        branch.labels.add(label);
        branch.nextBehaviours.put(label, null);
        return branch;
    }
}
