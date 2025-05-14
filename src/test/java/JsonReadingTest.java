import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonReadingTest {

    List<Class> initialPossibilities = List.of(
            SendInstr.class,
            ReceiveInstr.class,
            SelectInstr.class,
            BranchInstr.class,
            CdtInstr.class,
            CallInstr.class,
            EndInstr.class
    );

    @Test
    public void initialPossibilitiesAreAReducedSetOfInstruction(){
        var gen = new SPGenerator(16);
        assertEquals(gen.possibilities.size(), 16);
        assertTrue(gen.possibilities.stream()
                .allMatch(list -> list.stream()
                        .allMatch(instr -> initialPossibilities.contains(instr.getClass()))));
    }

    @Test
    public void test(){
        var gen = new SPGenerator(8);
    }
}
