package Behaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Comm extends Behaviour {
    Utils.Direction direction;
    public ArrayList<String> labels = new ArrayList<>();
    String destination;

    public Utils.Direction getDirection() {
        return direction;
    }

    public String getDestination() {
        return destination;
    }

    public Comm(String pr, String dest, Utils.Direction direction, List<String> labels){
        super(pr);
        this.destination= dest;
        this.direction = direction;
        this.labels.addAll(labels);
    }

    public Comm(String pr, String dest, Utils.Direction direction, String label){
        super(pr);
        this.destination= dest;
        this.direction = direction;
        if(!label.equals("")) {
            labels.add(label);
            nextBehaviours.put(label, null);
        }
    }

    public Comm(String pr, String dest, HashMap<String, Behaviour> branches){
        super(pr);
        this.destination = dest;
        this.direction = Utils.Direction.BRANCH;
        this.nextBehaviours = branches;
        this.labels = new ArrayList<>(branches.keySet().stream().toList());
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(direction.toString()).append(" ").append(process).append("-").append(destination);
        for (String key : nextBehaviours.keySet()) {
            if(key.contains(";")) {
                s.append(key).append("\n").append(nextBehaviours.get(key)
                        .toString());
            }else{
                s.append("\n\t").append(key).append(":").append("\n\t\t").append(nextBehaviours.get(key)
                        .toString().replace("\n","\n\t\t"));
            }
        }
        return s.toString();
    }
    @Override
    public boolean addBehaviour(Behaviour nb) {
        if (nextBehaviours.isEmpty()) {
            if(direction != Utils.Direction.BRANCH && direction != Utils.Direction.SELECT){
                nextBehaviours.put(";", nb);
            }else {
                nextBehaviours.put(labels.get(0), nb);
            }
            return true;
        }
        if(direction != Utils.Direction.BRANCH && direction != Utils.Direction.SELECT){
            return nextBehaviours.get(";").addBehaviour(nb);
        }else{
            var l = nextBehaviours.entrySet().stream().filter(entry -> entry.getValue() == null).toList();
            if(l.isEmpty()){
                nextBehaviours.get(labels.getLast()).addBehaviour(nb);
            }else{
                nextBehaviours.put(l.getFirst().getKey(), nb);
            }
        }
        return false;
    }

    @Override
    public Behaviour duplicate() {
        var c = new Comm(process, destination, direction, labels);
        if (!nextBehaviours.isEmpty()){
            if(direction != Utils.Direction.BRANCH && direction != Utils.Direction.SELECT) {
                c.nextBehaviours.put(";", nextBehaviours.get(";").duplicate());
            }else{
                for (String s : nextBehaviours.keySet()) {
                    c.nextBehaviours.put(s, nextBehaviours.get(s).duplicate());
                }
            }
        }
        return c;
    }

    @Override
    public boolean equals(Object b) {
        if(!(b instanceof Comm comm)) return false;
        if(!(comm.direction.equals(direction) &&
                comm.labels.equals(labels) &&
                comm.destination.equals(destination))) return false;
        return super.equals(b);
    }

    @Override
    public boolean isFinal() {
        if(direction != Utils.Direction.BRANCH){
            return false;
        }else{
            for (String s : nextBehaviours.keySet()) {
                if(!nextBehaviours.get(s).isFinal()) return false;
            }
            return true;
        }
    }



    @Override
    public List<Behaviour> getBranches() {
        if(direction == Utils.Direction.BRANCH){
            HashMap<String, List<Behaviour>> subBranches = new HashMap<>();
            for (String s : nextBehaviours.keySet()) {
                subBranches.put(s, nextBehaviours.get(s).getBranches());
            }
            return generateCombinations(subBranches);
        }else{
            List<Behaviour> branches = new ArrayList<>();
            for (String s : nextBehaviours.keySet()) {
                var br = nextBehaviours.get(s).getBranches();
                Behaviour b;
                for (Behaviour behaviour : br) {
                    b = duplicate();
                    b.nextBehaviours.put(s, behaviour);
                    branches.add(b);
                }
            }
            return branches;
        }
    }

    @Override
    public List<Behaviour> getLeaves() {
        if(nextBehaviours.containsKey(";")) return nextBehaviours.get(";").getLeaves();
        return List.of(this);
    }

    public List<Behaviour> generateCombinations(HashMap<String, List<Behaviour>> myHashMap) {
        List<Behaviour> myList = new ArrayList<>();
        List<Map.Entry<String, List<Behaviour>>> lists = new ArrayList<>(myHashMap.entrySet());
        generateCombinationsHelper(myList, lists, new HashMap<>(), 0);
        return myList;
    }

    public void generateCombinationsHelper(List<Behaviour> myList, List<Map.Entry<String, List<Behaviour>>> lists,
                                           HashMap<String, Behaviour> combination, int index) {
        if (index == lists.size()) {
            var b = duplicate();
            b.nextBehaviours = combination;
            myList.add(b.duplicate());
            return;
        }
        for (Behaviour behaviour : lists.get(index).getValue()) {
            var key = lists.get(index).getKey();
            combination.put(key, behaviour);
            generateCombinationsHelper(myList, lists, combination, index + 1);
        }
    }
}
