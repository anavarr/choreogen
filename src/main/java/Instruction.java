import Behaviour.Behaviour;

import java.util.List;

public interface Instruction {
    String name = "";
    static Instruction getIntrForRule(String s, String node, List<String> nodes) throws Exception {
        var possibleNodes = nodes.stream().filter(n -> !n.equals(node)).toList();
        return switch (s){
            case "rsend":   yield new SendInstr(possibleNodes);
            case "rrcv":    yield new ReceiveInstr(possibleNodes);
            case "rselect": yield new SelectInstr(possibleNodes);
            case "rbranch": yield new BranchInstr(possibleNodes);
            case "rif":     yield new IfInstr();
//            case "rcall":   yield new CallInstr(List.of());
            case "rend":    yield new EndInstr();
            default:
                throw new Exception("lqjdf");
        };
    }

    public List<String> getPossiblesNodes();
    public Boolean removePossibleNode(String node);

    public String getInstrName();

    public Behaviour generateBehaviour(int node, int range);
}
