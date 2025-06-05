import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void addRequirementBroadCast(Requirement req){if(req == this) return;
        if(instr instanceof EndInstr) throw new RuntimeException("can't add requirement to end ");
        if(nextRequirements.isEmpty()) nextRequirements.put(";", req);
        else{
            nextRequirements.entrySet().forEach(entry -> {
                if (entry.getValue() != null) entry.getValue().addRequirementBroadCast(req);
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
        var d = "";
        if(instr instanceof SendInstr si){
            d=si.destination;
        }else if(instr instanceof ReceiveInstr ris){
            d = ris.source;
        }else if(instr instanceof BranchInstr bis){
            d = bis.source;
        }
        for (Map.Entry<String, Requirement> e : nextRequirements.entrySet()) {
            if(s == null) continue;
            else s = s+" "+d+"\n\t"+e.getKey()+"\n\t\t"+e.getValue().toString();
        }
        return s;
    }
}
