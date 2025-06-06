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
    public void sendReceiveOnly(){
        var gen = new SPGenerator(10, "rules_valid_min.json");
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
        var gen = new SPGenerator(3000, "rules_valid_min_cdt.json");
        gen.generateSystem();
        var writer = new SPWriter();
        for (String s : gen.system.keySet()) {
            try {
                writer.write(s, gen.system.get(s));
            } catch (Exception e) {
                var item = gen.system.get(s);
                throw new RuntimeException(e);
            }
        }
        writer.writeIt();
    }
}
