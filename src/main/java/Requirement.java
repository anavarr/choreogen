import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        if(req == this) return;
        if(instr instanceof EndInstr) throw new RuntimeException("can't add requirement to end ");
        if(nextRequirements.isEmpty()) nextRequirements.put(";", req);
        else{
            nextRequirements.entrySet().forEach(entry -> {
                if (entry.getValue() != null) entry.getValue().addRequirement(req);
                else nextRequirements.put(entry.getKey(), req);
            });
        }
    }

    public void setRequirements(HashMap<String, Requirement> req){
        this.nextRequirements = req;
    }

    public boolean hasRequirementId(int id){
        if(originId == id) return true;
        return nextRequirements.values().stream().anyMatch(el -> el.hasRequirementId(id));
    }

    public List<Requirement> getRequirementChainUntil(Requirement lastRequirement) {
        ArrayList<Requirement> list = new ArrayList<>();
        if(this == lastRequirement) return List.of(this);
        for (Requirement value : nextRequirements.values()) {
            if(value == null) continue;
            var l = value.getRequirementChainUntil(lastRequirement);
            if(!l.isEmpty()){
                list.add(this);
                list.addAll(l);
            }
        }
        return list;
    }

    public Instruction getInstr() {
        return instr;
    }

    @Override
    public String toString() {
        String s = instr.getInstrName();
        for (Requirement value : nextRequirements.values()) {
            if(s == null) continue;
            else s = s+"\n\t"+value.toString();
        }
        return s;
    }
}
