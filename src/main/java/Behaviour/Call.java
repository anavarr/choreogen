package Behaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Call extends Behaviour{
    String variableName;
    public Call(String pr) {
        super(pr);
    }

    @Override
    public boolean addBehaviour(Behaviour nb) {
        if (nextBehaviours.isEmpty()){
            nextBehaviours.put("unfold", nb);
            return true;
        }
        return nextBehaviours.get("unfold").addBehaviour(nb);
    }

    public Call(String pr, HashMap<String, Behaviour> nb) throws IllegalArgumentException {
        super(pr, nb);
        if(nb.size() > 1 || (nb.size() == 1 && !nb.containsKey("unfold"))){
            throw new IllegalArgumentException("A Call Behaviour can have only one continuation mapped to the 'unfold' keyword");
        }
    }

    public Call(String pr, String vn){
        this(pr);
        variableName = vn;
    }

    public Call(String pr, HashMap<String, Behaviour> nb, String vn) throws IllegalArgumentException{
        this(pr, nb);
        variableName = vn;
    }

    public String getVariableName(){
        return variableName;
    }

    @Override
    public String toString() {
        var s = "Call "+variableName+"\n";
        return s+super.toString();
    }


    @Override
    public boolean equals(Object b) {
        if(!(b instanceof Call call && call.variableName.equals(variableName))) return false;
        return super.equals(b);
    }

    @Override
    public Behaviour duplicate() {
        if(nextBehaviours.isEmpty()){
            return new Call(process, variableName);
        }else{
            var nb = nextBehaviours.get("unfold").duplicate();
            var hm = new HashMap<String, Behaviour>();
            hm.put("unfold", nb);
            return new Call(process, hm, variableName);
        }
    }

    @Override
    public boolean isFinal() {
        //If no nextBehaviour exist it means we already unfolded this one and we don't need to do it once more
        return false;
    }

    @Override
    public List<Behaviour> getBranches() {
        if(!nextBehaviours.containsKey("unfold")) return List.of(this);

        var branches = new ArrayList<Behaviour>();
        var br = nextBehaviours.get("unfold").getBranches();
        for (Behaviour behaviour : br) {
            var t = duplicate();
            t.nextBehaviours.put("unfold", behaviour);
            branches.add(t);
        }
        return branches;
    }

    @Override
    public List<Behaviour> getLeaves() {
        return List.of();
    }
}
