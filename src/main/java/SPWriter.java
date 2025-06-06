import Behaviour.Behaviour;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import Behaviour.Comm;
import Behaviour.Cdt;
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
            genCtx.prgm.getLast().append("\n|\n");
        }
        genCtx.prgm.getLast().append("\n").append(indexToLetter(process)).append("[");
        switchIt(root,"\t");
        genCtx.prgm.getLast().append("\n]");
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

    private void switchIt(Behaviour root, String prefix) throws Exception {
        var currentString = genCtx.prgm.getLast();
        switch (root){
//            case Call call:
//                if(call.nextBehaviours.isEmpty()){
//                }else{
//                }
//                break;
            case Cdt cdt:
                //merge it
                currentString.append(prefix+"\n");
                currentString.append(prefix+"If ").append(cdt.getExpr()).append(" Then ");
                switchIt(cdt.nextBehaviours.get("then"), prefix+"\t");
                currentString.append("\n").append(prefix).append("Else");
                switchIt(cdt.nextBehaviours.get("else"), prefix+"\t");
                break;
            case Comm comm:
                switch (comm.getDirection()){
                    default -> {
                        System.out.println("weird");
                    }
                    case Direction.VOID, Direction.DUMMY -> throw new IllegalArgumentException();
                    case Direction.SEND -> {
                        currentString
                                .append("\n")
                                .append(prefix).append(indexToLetter(comm.getDestination())).append("!").append("myVar")
                                .append("@!\"\";");
                        if(!comm.nextBehaviours.isEmpty()) switchIt(comm.nextBehaviours.get(";"),prefix);
                    }
                    case Direction.RECEIVE -> {
                        currentString
                                .append("\n")
                                .append(prefix).append(indexToLetter(comm.getDestination())).append("?").append("myVar")
                                .append("@?\"\";");
                        if(!comm.nextBehaviours.isEmpty()) switchIt(comm.nextBehaviours.get(";"), prefix);
                    }
                    case Direction.BRANCH -> {
                        currentString.append("\n")
                                .append(prefix).append(indexToLetter(comm.getDestination())).append("&");
                        int counter=0;
                        for (String s : comm.nextBehaviours.keySet()) {
                            currentString.append("\n").append(prefix).append("{\"").append(s).append("\" :Some(");
                            switchIt(comm.nextBehaviours.get(s), prefix+"\t");
                            currentString.append("\n").append("\t").append(")}");
                            counter++;
                            if(counter < comm.nextBehaviours.size()) currentString.append("\n").append(prefix).append("//");
                        }
                    }
                    case Direction.SELECT -> {
                        var label = comm.labels.getFirst();
                        currentString
                                .append("\n")
                                .append(prefix)
                                .append(indexToLetter(comm.getDestination())).append("+").append("\"")
                                .append(label).append("\"")
                                .append("@+\"\";");
                        if(!comm.nextBehaviours.isEmpty()) switchIt(comm.nextBehaviours.get(label), prefix);
                    }
                };
                break;
            case End end:
                genCtx.prgm.getLast().append("\n").append(prefix).append("End");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + root);
        };

    }
}