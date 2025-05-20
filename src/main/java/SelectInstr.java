import Behaviour.Behaviour;

import java.util.List;
import Behaviour.Comm;
import Behaviour.Utils;

public class SelectInstr extends CommInstr implements Instruction{
    String instrName = "rselect";
    String destination;
    String label;
    static int labelCounter = 0;

    public SelectInstr(String snode, String label) {
        this.destination = snode;
        this.label = label;
    }

    public SelectInstr(List<String> possibleNodes) {
        this.possibleNodes = possibleNodes;
    }

    @Override
    public String getInstrName() {
        return instrName;
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        return new Comm(String.valueOf(node), randomPick(), Utils.Direction.SELECT, generateLabel());
    }

    private String generateLabel(){
        labelCounter++;
        return "label"+labelCounter;
    }
}
