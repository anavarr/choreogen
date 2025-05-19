import Behaviour.Behaviour;

import java.util.List;

public interface Instruction {
    static Instruction getIntrForRule(String s, String node, List<String> nodes) throws Exception {
        return switch (s){
            case "rsend":   yield new SendInstr(nodes.stream().filter(n -> !n.equals(node)).toList());
            case "rrcv":    yield new ReceiveInstr(nodes.stream().filter(n -> !n.equals(node)).toList());
//            case "rselect": yield new SelectInstr();
//            case "rbranch": yield new BranchInstr();
//            case "rlabel":  yield new BranchInstr();    // should retrieve the one for rbranch
//            case "rif":     yield new CdtInstr();
//            case "relse":   yield new CdtInstr();       // should retrieve the one for rif
            case "rcall":   yield new CallInstr(List.of());
            case "rend":    yield new EndInstr();
            default:
                throw new Exception("lqjdf");
        };
    }

    public List<String> getPossiblesNodes();
    public Boolean removePossibleNode(String node);

    public Behaviour generateBehaviour(int node, int range);
}
