import java.util.ArrayList;
import java.util.List;

public class Requirement {
    static String scopeNameCounter = "main";
    String scopeName;
    List<Requirement> nextRequirements;
    Instruction instr;

    public Requirement(Instruction instr){
        this.instr = instr;
        this.nextRequirements = new ArrayList<>();
    }

    public void addRequirement(Requirement req){
        if(nextRequirements.isEmpty()) nextRequirements.add(req);
        else nextRequirements.getFirst().addRequirement(req);
    }
}
