import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonReadingTest {

    List<Class> initialPossibilities = List.of(
            SendInstr.class,
            ReceiveInstr.class,
            SelectInstr.class,
            BranchInstr.class,
            IfInstr.class,
            CallInstr.class,
            EndInstr.class
    );

    String path = "/tmp";
    String name = "choreoGen";

    @BeforeEach
    public void cleanOutput() throws IOException {
        deleteDir(new File(path+"/"+name));
        assertFalse(Files.exists(Path.of(path, name)));
    }

    void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    @Test
    public void initialPossibilitiesAreAReducedSetOfInstruction(){
        var gen = new SPGenerator(16);
        gen.computeInitialPossibilities();
        assertEquals(gen.possibilities.size(), 16);
        assertTrue(gen.possibilities.stream()
                .allMatch(list -> list.stream()
                        .allMatch(instr -> initialPossibilities.contains(instr.getClass()))));
    }

    @Test
    public void sendReceiveOnly(){
        var gen = new SPGenerator(1000, "rules_valid_min.json");
        gen.computeInitialPossibilities();
        assertTrue(gen.possibilities.stream().allMatch(list -> list.size() == 3));
        gen.generateSystem();
        var writer = new SPWriter();
        for (String s : gen.system.keySet()) {
            try {
                writer.write(s, gen.system.get(s));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        writer.writeIt();
    }

    @Test
    public void sendReceiveCallDefOnly(){
        var gen = new SPGenerator(8, "rules_valid_min_call.json");
        gen.computeInitialPossibilities();
        assertTrue(gen.possibilities.stream().allMatch(list -> list.size() == 4));
        gen.generateSystem();
        var writer = new SPWriter();
        for (String s : gen.system.keySet()) {
            try {
                writer.write(s, gen.system.get(s));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        writer.writeIt();
    }

    @Test
    public void allButRecursion(){
        var gen = new SPGenerator(8, "rules_valid_min_select_branch_if.json");
        gen.computeInitialPossibilities();
        assertTrue(gen.possibilities.stream().allMatch(list -> list.size() == 6));
        gen.generateSystem();
        var writer = new SPWriter();
        for (String s : gen.system.keySet()) {
            try {
                writer.write(s, gen.system.get(s));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        writer.writeIt();
    }
}
