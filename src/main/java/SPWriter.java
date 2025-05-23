import Behaviour.Behaviour;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import Behaviour.Comm;
import Behaviour.Utils.Direction;
import Behaviour.End;
public class SPWriter {
    Behaviour root;
    Path folder;
    String process;
    GenerationContext genCtx;

    class GenerationContext{
        ArrayList<StringBuilder> prgm = new ArrayList<>();
    }

    public SPWriter(){
        this.genCtx = new GenerationContext();
    }

    public boolean write(String process, Behaviour root) throws Exception {
        return this.write(process, root, Path.of("/","tmp", "choreoGen"));
    }

    private String indexToLetter(String i){
        return "process"+i;
//        if(Integer.parseInt(i) < 26) return String.valueOf((char)(Integer.parseInt(i) + 97));
//        else{
//            StringBuilder name = new StringBuilder();
//            int l = Integer.parseInt(i);
//            while(l > 0){
//                var sub = Math.min(25,l);
//                name.append(indexToLetter(String.valueOf(sub)));
//                l-=sub;
//            }
//            return name.toString();
//        }
    }

    public boolean write(String process, Behaviour root, Path folder) throws Exception {
        this.root = root;
        this.folder = folder;
        this.process = process;
        genCtx.prgm.add(new StringBuilder());
        if (genCtx.prgm.size() > 1) {
            genCtx.prgm.getLast().append("|");
        }
        genCtx.prgm.getLast().append("\n").append(indexToLetter(process)).append("[");
        switchIt(root);
        genCtx.prgm.getLast().append("]");
        return true;
    }

    public void writeIt(){
        var absoluteName = folder.toString()+"/"+"system.sp";
        try {
            Files.createDirectories(folder);
            var systemString = genCtx.prgm.stream().reduce(new StringBuilder(), StringBuilder::append).toString();
            Files.write(Path.of(absoluteName), systemString.getBytes());
            System.out.println("Successfully wrote to "+absoluteName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void switchIt(Behaviour root) throws Exception {
        var currentString = genCtx.prgm.getLast();
        switch (root){
//            case Call call:
//                if(call.nextBehaviours.isEmpty()){
//                }else{
//                }
//                break;
//            case Cdt cdt:
//                //merge it
//                var branches = cdt.getImmediateBranches();
//                if(branches.isEmpty()){
//
//                }
//                //two cases : first one is a selection, none is
//                var selectBranches = branches.stream()
//                        .filter(el -> {
//                            if(el instanceof Comm comm){
//                                return comm.getDirection().equals(Utils.Direction.SELECT);
//                            }
//                            return false;
//                        });
//                if(selectBranches.count() == branches.size()){
//                    //all select
//                    // check that they all have same destination !!!
//                    var destinations = new HashSet<>(branches.stream().map(el -> ((Comm)el).getDestination()).toList());
//                    if(destinations.size() > 1) {
//                        throw new Exception("Can't extract local type as all processes are not selected");
//                    }
//                }else{
//                    //not all select, it is not great
//                }
//                break;
            case Comm comm:
                switch (comm.getDirection()){
                    default -> {
                        System.out.println("weird");
                    }
                    case Direction.VOID, Direction.DUMMY -> throw new IllegalArgumentException();
                    case Direction.SEND -> {
                        currentString
                                .append("\n")
                                .append(indexToLetter(comm.getDestination())).append("!").append("myVar")
                                .append("@!\"\";");
                        if(!comm.nextBehaviours.isEmpty()) switchIt(comm.nextBehaviours.get(";"));
                    }
                    case Direction.RECEIVE -> {
                        currentString
                                .append("\n")
                                .append(indexToLetter(comm.getDestination())).append("?").append("myVar")
                                .append("@?\"\";");
                        if(!comm.nextBehaviours.isEmpty()) switchIt(comm.nextBehaviours.get(";"));
                    }
                    case Direction.BRANCH -> {
                        currentString.append("\n")
                                .append(indexToLetter(comm.getDestination())).append("&");
                        int counter=0;
                        for (String s : comm.nextBehaviours.keySet()) {
                            currentString.append("\n{").append("\"").append(s).append("\" :");
                            switchIt(comm.nextBehaviours.get(s));
                            currentString.append("\n").append("}");
                            counter++;
                            if(counter < comm.nextBehaviours.size()) currentString.append("//");
                        }
                    }
                    case Direction.SELECT -> {
                        var label = comm.labels.getFirst();
                        currentString
                                .append("\n")
                                .append(indexToLetter(comm.getDestination())).append("+").append("\"")
                                .append(label).append("\"")
                                .append("@+\"\";");
                        if(!comm.nextBehaviours.isEmpty()) switchIt(comm.nextBehaviours.get(label));
                    }
                };
                break;
            case End end:
                genCtx.prgm.getLast().append("\n End");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + root);
        };

    }
}