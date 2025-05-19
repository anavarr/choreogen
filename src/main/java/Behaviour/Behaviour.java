package Behaviour;

import java.util.HashMap;
import java.util.List;

public abstract class Behaviour {
    public HashMap<String, Behaviour> nextBehaviours = new HashMap<>();
    String process;
    public Behaviour(String pr){
        process = pr;
    }
    public Behaviour(String pr, HashMap<String, Behaviour> nb){
        this(pr);
        nextBehaviours = nb;
    }
    public abstract boolean addBehaviour(Behaviour nb);


    abstract public Behaviour duplicate();
    @Override
    public String toString() {
        if(nextBehaviours == null) return "";
        return String.join("\n",nextBehaviours.keySet().stream()
                .map(item -> nextBehaviours.get(item).toString().replace("\n","\n\t"))
                .toList()
        );
    }

    @Override
    public boolean equals(Object b1){
        if (!(b1 instanceof Behaviour b && b.process.equals(process))) return false;
        if(!(b).nextBehaviours.keySet().containsAll(nextBehaviours.keySet()) ||
                !nextBehaviours.keySet().containsAll((b).nextBehaviours.keySet())) return false;
        for (String s : nextBehaviours.keySet()) {
            if(!nextBehaviours.get(s).equals((b).nextBehaviours.get(s))) return false;
        }
        return true;
    }

    abstract public boolean isFinal();

    abstract public List<Behaviour> getBranches();
    abstract public List<Behaviour> getLeaves();
}
