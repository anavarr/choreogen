import java.util.ArrayList;
import java.util.List;

public abstract class CommInstr implements Instruction{

    List<String> possibleNodes = new ArrayList<>();

    public String randomPick(){
        if(possibleNodes.isEmpty()) return null;
        int index = (int)Math.round(Math.random()*(possibleNodes.size()-1));
        return possibleNodes.get(index);
    }

    @Override
    public List<String> getPossiblesNodes() {
        return possibleNodes;
    }

    @Override
    public Boolean removePossibleNode(String node) {
        return possibleNodes.remove(node);
    }
}
