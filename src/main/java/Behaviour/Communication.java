package Behaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import  static Behaviour.Utils.Direction.BRANCH;
import  static Behaviour.Utils.Direction.RECEIVE;
import  static Behaviour.Utils.Direction.SELECT;
import  static Behaviour.Utils.Direction.SEND;
import  static Behaviour.Utils.Direction.VOID;

public class Communication {
    private final Utils.Direction direction;
    private final ArrayList<Communication> nextCommunicationNodes = new ArrayList<>();
    private final String label;
    private final ArrayList<Communication> previousCommunicationNodes = new ArrayList<>();
    private final ArrayList<Communication> recursiveCallees = new ArrayList<>();
    private final HashMap<Communication, Integer> visitedRecursiveBranches = new HashMap<>();

    public Communication(Utils.Direction direction, ArrayList<Communication> nextNodes, String label, ArrayList<Communication> previousNodes){
        Objects.requireNonNull(direction);
        this.direction = direction;
        previousCommunicationNodes.addAll(previousNodes);
        this.label = label;
        if(direction.equals(SELECT) || direction.equals(Utils.Direction.BRANCH)){
            Objects.requireNonNull(label);
        }
        for (Communication comBranch : nextNodes) {
            if(nodeIsSelfOrAbove(comBranch)) addRecursiveCallee(comBranch);
            else {
                nextCommunicationNodes.add(comBranch);
                comBranch.previousCommunicationNodes.add(this);
            }
        }
    }

    public Utils.Direction getDirection() {
        return direction;
    }
    public String getLabel(){
        return label;
    }

    public Communication(Utils.Direction direction, String label){
        this(direction, new ArrayList<>(), label, new ArrayList<>());
    }
    public Communication(Utils.Direction direction, ArrayList<Communication> nextCommunicationNodes){
        this(direction, nextCommunicationNodes, null, new ArrayList<>());
    }
    public Communication(Utils.Direction direction, String label, ArrayList<Communication> nextCommunicationNodes){
        this(direction, nextCommunicationNodes, label, new ArrayList<>());
    }
    public Communication(Utils.Direction direction, Communication nextCommunication){
        this(direction, new ArrayList<>(List.of(nextCommunication)));
    }
    public Communication(Utils.Direction direction){
        this(direction, new ArrayList<>());
    }



    public boolean isSend(){
        return direction == Utils.Direction.SEND;
    }

    public boolean isReceive(){
        return direction == Utils.Direction.RECEIVE;
    }

    public boolean isSelect(){
        return direction == SELECT;
    }

    public boolean isBranch(){
        return direction == Utils.Direction.BRANCH;
    }
    public void addRecursiveCallee(Communication c){
        var canAdd = true;
        for (Communication recursiveCallee : recursiveCallees) {
            if(recursiveCallee == c) {
                canAdd = false;
                break;
            }
        }
        if(canAdd) recursiveCallees.add(c);
    }
    public List<Communication> getRecursiveCallees(){
        return recursiveCallees;
    }

    public int getBranchesSize(){
        int c = 0;
        for (Communication communicationsBranch : nextCommunicationNodes) {
            c+=communicationsBranch.getBranchesSize();
        }
        return c+1;
    }
    public boolean isComplementary(Communication comp){
        if (!(isSend() ? comp.isReceive() :
                (isReceive() ? comp.isSend() :
                        (isBranch() ? comp.isSelect() :
                                isSelect() && comp.isBranch())))) return false;
        if(nextCommunicationNodes.size() != comp.nextCommunicationNodes.size()) return false;
        if(!Objects.equals(label, comp.label)) return false;
        for (int i = 0; i < nextCommunicationNodes.size(); i++) {
            var c1 = nextCommunicationNodes.get(i);
            var c2 = comp.nextCommunicationNodes.get(i);
            if(!c1.isComplementary(c2)) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if (!(o instanceof Communication comp)) return false;
        if(!(direction == comp.direction && Objects.equals(label, comp.label))) return false;
        //handling recursive branches
        if(nextCommunicationNodes.size() + recursiveCallees.size()
                != comp.nextCommunicationNodes.size() + comp.recursiveCallees.size()) return false;

        for(int i = 0; i < nextCommunicationNodes.size(); i++){
            if(!comp.nextCommunicationNodes.contains(nextCommunicationNodes.get(i))){
                 if(!comp.recursiveCallees.contains(nextCommunicationNodes.get(i))) return false;
            }
        }
        if(!recursiveCallees.isEmpty()){
            if(visitedRecursiveBranches.containsKey(this)){
                var recursiveIndex = visitedRecursiveBranches.get(this);
                if(recursiveIndex < recursiveCallees.size()){
                    visitedRecursiveBranches.put(this, recursiveIndex+1);
                    if(!comp.recursiveCallees.contains(recursiveCallees.get(recursiveIndex))){
                        return comp.nextCommunicationNodes.contains(recursiveCallees.get(recursiveIndex));
                    }else{
                        return true;
                    }
                }
            }else{
                visitedRecursiveBranches.put(this, 1);
                if(!comp.recursiveCallees.contains(recursiveCallees.getFirst())){
                    return comp.nextCommunicationNodes.contains(recursiveCallees.getFirst());
                }else{
                    return true;
                }
            }
        }
//        if(!recursiveCallees.isEmpty()){
//            if(visitedRecursiveBranches.containsKey(this)){
//                var recursiveIndex = visitedRecursiveBranches.get(this);
//                if(recursiveIndex < recursiveCallees.size()){
//                    visitedRecursiveBranches.put(this, recursiveIndex+1);
//                    return comp.recursiveCallees.contains(recursiveCallees.get(recursiveIndex));
//                }
//            }else{
//                visitedRecursiveBranches.put(this, 1);
//                return comp.recursiveCallees.contains(recursiveCallees.get(0));
//            }
//        }
//        for (int i = 0; i < nextCommunicationNodes.size(); i++) {
//            if(!(nextCommunicationNodes.contains(comp.nextCommunicationNodes.get(i))
//                    & comp.nextCommunicationNodes.contains(nextCommunicationNodes.get(i)))) return false;
//        }
        return true;
    }

    public void resetVisitedRecursiveBranches(){
        visitedRecursiveBranches.clear();
        for (Communication nextCommunicationNode : nextCommunicationNodes) {
            nextCommunicationNode.resetVisitedRecursiveBranches();
        }
    }

    public boolean isBranchingValid() {
        if(nextCommunicationNodes.isEmpty()) return true;
        //if the branches are all SELECT, then anything following is valid
        var allSelect = nextCommunicationNodes.stream()
                .allMatch(item -> item.direction == SELECT);
        //if the branches are all BRANCH, then anything following is valid
        var allBranch = nextCommunicationNodes.stream()
                .allMatch(item -> item.direction == Utils.Direction.BRANCH);
        //else, branching is valid if all branches are the same
        var allSame = nextCommunicationNodes.stream()
                .allMatch(item -> item.equals(nextCommunicationNodes.getFirst()));
        if(!(allSelect || allBranch || allSame)) return false;
        return nextCommunicationNodes.stream().allMatch(Communication::isBranchingValid);
    }

    public boolean nodeIsBelow(Communication node){
        for (Communication nextCommunicationNode : nextCommunicationNodes)
            if(nextCommunicationNode == node || nextCommunicationNode.nodeIsBelow(node)) return true;
        return false;
    }
    public boolean nodeIsSelfOrBelow(Communication node){
        if(node == this) return true;
        for (Communication nextCommunicationNode : nextCommunicationNodes)
            if(nextCommunicationNode.nodeIsSelfOrBelow(node)) return true;
        return false;
    }
    public boolean nodeIsAbove(Communication root){
        for (Communication communication : previousCommunicationNodes)
            if(communication == root || communication.nodeIsAbove(root)) return true;
        return false;
    }
    public boolean nodeIsSelfOrAbove(Communication root){
        if(root == this) return true;
        for (Communication communication : previousCommunicationNodes) {
            if(communication == root || communication.nodeIsSelfOrAbove(root)) return true;
        }
        return false;
    }
    public boolean changeNextToRecursive(Communication target){
        var changed = false;
        for (Communication nextCommunicationNode : nextCommunicationNodes) {
            if(nextCommunicationNode == target) {
                nextCommunicationNodes.remove(nextCommunicationNode);
                recursiveCallees.add(nextCommunicationNode);
                changed = true;
            } else {
                if(nextCommunicationNode.changeNextToRecursive(target)) changed = true;
            }
        }
        return false;
    }
    public void addLeafCommunicationRoots(ArrayList<Communication> roots){
        if(nextCommunicationNodes.isEmpty()){
            for (Communication root : roots) {
                if(nodeIsSelfOrAbove(root)) addRecursiveCallee(root);
                else {
                    //check that roots chain aren't previous communications
                    if(direction == VOID){
                        for (Communication previousCommunicationNode : previousCommunicationNodes) {
                            previousCommunicationNode.nextCommunicationNodes.add(root);
                            previousCommunicationNode.nextCommunicationNodes.remove(this);
                        }
                        root.previousCommunicationNodes.addAll(previousCommunicationNodes);
                    }else{
                        nextCommunicationNodes.add(root);
                        root.previousCommunicationNodes.add(this);
                    }
                }
            }
        }else{
            nextCommunicationNodes.get(0).addLeafCommunicationRoots(roots);
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Communication").append(" ").append(this.hashCode()).append("[\n");
        s.append(String.format("\tdirection=%s\n\tlabel=%s\n\trecursive calls=%s\n\t[",
                direction, label, recursiveCallees.stream().map((it -> it.hashCode())).toList()));
        for (Communication communicationsRoot : nextCommunicationNodes) {
            s.append("\n\t\t").append(communicationsRoot.toString().replace("\n", "\n\t\t"));
        }
        return s.append("\n\t]\n]").toString();
    }

    public ArrayList<Communication> getLeaves() {
        if(nextCommunicationNodes.isEmpty()){
            return new ArrayList<>(List.of(this));
        }
        return nextCommunicationNodes.stream().map(Communication::getLeaves).reduce(new ArrayList<>(),
                (acc, it) -> {acc.addAll(it); return acc;});
    }

    public Set<String> getDirectedLabels(Utils.Direction dir) {
        var labels = new HashSet<String>();
        if(label != null && direction == dir) labels.add(label);
        for (Communication communicationsBranch : nextCommunicationNodes)
            labels.addAll(communicationsBranch.getDirectedLabels(dir));
        return labels;
    }

    public boolean containsDirectNextNode(Communication com){
        return nextCommunicationNodes.contains(com);
    }
    public boolean containsDirectPreviousNode(Communication com) { return previousCommunicationNodes.contains(com); }

    public boolean supports(Communication targetNode) {
        if(direction == SEND && targetNode.direction != SEND  && targetNode.direction != SELECT) return false;
        if(direction == SELECT && targetNode.direction != SEND  && targetNode.direction != SELECT) return false;
        if(direction == RECEIVE && targetNode.direction != RECEIVE && targetNode.direction != BRANCH) return false;
        if(direction == BRANCH && targetNode.direction != RECEIVE && targetNode.direction != BRANCH) return false;
        if(direction == VOID && targetNode.direction != VOID) return false;
        // If target node is in final state, we need to make sure the template can reach final state
        if(targetNode.isFinal() && !canBeFinal()) return false;
        // If template can only reach final state, need to make sure that node is final state
        if(isFinal()  && !targetNode.isFinal()) return false;


        for (Communication nextTargetCommunicationNode : targetNode.nextCommunicationNodes) {
            var supported = !nextCommunicationNodes.stream()
                    .filter(node -> node.supports(nextTargetCommunicationNode)).toList().isEmpty();

            if(!supported){
                supported = !recursiveCallees.stream()
                        .filter(node -> node.supports(nextTargetCommunicationNode)).toList().isEmpty();
            }
            if(!supported) return false;
        }
        for (Communication recursiveCallee : targetNode.recursiveCallees) {
            var supported = !nextCommunicationNodes.stream()
                    .filter(node -> node.supports(recursiveCallee)).toList().isEmpty();
            if(!supported){
                for (Communication recursiveCommunicationNode : recursiveCallees) {
                    if(visitedRecursiveBranches.containsKey(this)){
                        var recursiveIndex = visitedRecursiveBranches.get(this);
                        if(recursiveIndex < recursiveCallees.size()){
                            visitedRecursiveBranches.put(this, recursiveIndex+1);
                            if(recursiveCommunicationNode.supports(recursiveCallee)){
                                supported = true;
                                break;
                            }
                        }else{
                            supported = true;
                            break;
                        }
                    }else{
                        visitedRecursiveBranches.put(this, 1);
                        if(recursiveCommunicationNode.supports(recursiveCallee)){
                            supported = true;
                            break;
                        }
                    }
                }
            }
            if(!supported) return false;
        }
        return true;
    }

    public boolean hasNextNodes(){
        return !nextCommunicationNodes.isEmpty();
    }

    public boolean hasPreviousNodes(){
        return !previousCommunicationNodes.isEmpty();
    }

    public void cleanVoid() {
        for (Communication nextCommunicationNode : nextCommunicationNodes)
            nextCommunicationNode.cleanVoid();
        if(recursiveCallees.isEmpty() &&
                nextCommunicationNodes.size() == 1 && nextCommunicationNodes.get(0).direction == VOID){
            nextCommunicationNodes.get(0).previousCommunicationNodes.remove(this);
            nextCommunicationNodes.remove(0);
        }
    }

    public boolean canBeFinal(){
        for (Communication nextCommunicationNode: nextCommunicationNodes){
            if(nextCommunicationNode.direction == VOID) return true;
        }
        return nextCommunicationNodes.isEmpty() && recursiveCallees.isEmpty();
    }
    public boolean isFinal(){
        for (Communication nextCommunicationNode : nextCommunicationNodes) {
            if(nextCommunicationNode.direction != VOID) return false;
        }
        return nextCommunicationNodes.isEmpty() && recursiveCallees.isEmpty();
    }

    public void addNextCommunicationNodes(ArrayList<Communication> communications) {
        nextCommunicationNodes.addAll(communications);
        for (Communication communication : communications) {
            communication.previousCommunicationNodes.add(this);
        }
    }

    public List<Communication> getNextCommunicationNodes(){
        return nextCommunicationNodes;
    }

    public List<Communication> getRecursiveCallersTo(Communication comm) {
        var recursiveCallersTo = new ArrayList<Communication>();
        if(recursiveCallees.contains(comm)) recursiveCallersTo.add(this);
        for (Communication nextNode : nextCommunicationNodes) {
            recursiveCallersTo.addAll(nextNode.getRecursiveCallersTo(comm));
        }
        return recursiveCallersTo;
    }

    public void removeInPlace(Communication dummy) {
        for (Communication nextCommunicationNode : nextCommunicationNodes) {
            nextCommunicationNode.removeInPlace(dummy);
        }
        var wasHere = nextCommunicationNodes.remove(dummy);
        if(wasHere){
            nextCommunicationNodes.addAll(dummy.nextCommunicationNodes);
            nextCommunicationNodes.addAll(dummy.getNextCommunicationNodes());
            recursiveCallees.addAll(dummy.getRecursiveCallees());
        }

        wasHere = recursiveCallees.remove(dummy);
        if(wasHere){
            recursiveCallees.remove(dummy);
            recursiveCallees.addAll(dummy.getNextCommunicationNodes());
            recursiveCallees.addAll(dummy.getRecursiveCallees());
        }
    }

    public void replace2(Communication toReplace, Communication replacer){
        var wasHere = nextCommunicationNodes.remove(toReplace);
        if(wasHere){
            recursiveCallees.add(replacer);
        }
        wasHere = recursiveCallees.remove(toReplace);
        if(wasHere) recursiveCallees.add(replacer);

        for (Communication nextCommunicationNode : nextCommunicationNodes) {
            nextCommunicationNode.replace2(toReplace, replacer);
        }
    }

    public void replace(Communication toReplace, Communication replacer) {
        var wasHere = nextCommunicationNodes.remove(toReplace);
        if(wasHere){
            if(nodeIsAbove(replacer)){
                recursiveCallees.add(replacer);
            }else if(nodeIsBelow(replacer)){
                System.out.println("probel");
            }else{
                nextCommunicationNodes.add(replacer);
            }
        }
        wasHere = recursiveCallees.remove(toReplace);
        if(wasHere) recursiveCallees.add(replacer);
        for (Communication nextCommunicationNode : nextCommunicationNodes) {
            nextCommunicationNode.replace(toReplace, replacer);
        }
    }
}
