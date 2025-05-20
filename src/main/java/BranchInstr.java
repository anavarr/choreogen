import Behaviour.Behaviour;
import jdk.jshell.spi.ExecutionControl;

import java.util.List;

public class BranchInstr implements Instruction{

    String source;
    List<String> possibleNodes;

    public BranchInstr(String source){
        this.source = source;
    }

    public BranchInstr(List<String> possibleNodes){
        this.possibleNodes = possibleNodes;
    }

    @Override
    public List<String> getPossiblesNodes() {
        return possibleNodes;
    }

    @Override
    public Boolean removePossibleNode(String node) {
        if(possibleNodes == null) return false;
        return possibleNodes.remove(node);
    }

    @Override
    public Behaviour generateBehaviour(int node, int range) {
        System.err.println("behaviour generation for branch instruction is not implemented");
    }
}
