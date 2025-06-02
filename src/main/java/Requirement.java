import java.util.ArrayList;
import java.util.HashMap;

public class Requirement {
    static String scopeNameCounter = "main";
    String scopeName;
    HashMap<String, Requirement> nextRequirements;
    Instruction instr;

    public Requirement(Instruction instr){
        this.instr = instr;
        this.nextRequirements = new HashMap<>();
    }

    public void addRequirement(Requirement req){
        if(instr instanceof EndInstr) throw new RuntimeException("can't add requirement to end ");
        if(nextRequirements.isEmpty()) nextRequirements.put(";", req);
        else nextRequirements.get(";").addRequirement(req);
    }

    public void setRequirements(HashMap<String, Requirement> req){
        this.nextRequirements = req;
    }
}
