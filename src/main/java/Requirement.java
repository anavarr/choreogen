import java.util.List;

public class Requirement {
    static String scopeNameCounter = "main";
    String scopeName;
    List<Requirement> nextRequirements;
    Instruction instr;
}
