import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Requirement {
    static String scopeNameCounter = "main";
    String scopeName;
    HashMap<String, Requirement> nextRequirements;
    Instruction instr;
    int originId;

    public Requirement(Instruction instr, int id){
        this.instr = instr;
        this.nextRequirements = new HashMap<>();
        this.originId = id;
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
