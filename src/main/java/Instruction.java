public interface Instruction {
    static Instruction getIntrForRule(String s) throws Exception {
        return switch (s){
            case "rsend":   yield new SendInstr();
            case "rrcv":    yield new ReceiveInstr();
            case "rselect": yield new SelectInstr();
            case "rbranch": yield new BranchInstr();
            case "rlabel":  yield new BranchInstr();    // should retrieve the one for rbranch
            case "rif":     yield new CdtInstr();
            case "relse":   yield new CdtInstr();       // should retrieve the one for rif
            case "rcall":   yield new CallInstr();
            case "rdef":    yield new DefInstr();
            case "rend":    yield new EndInstr();
            default:
                throw new Exception("lqjdf");
        };
    }
}
