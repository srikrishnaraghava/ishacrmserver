package crmdna.programtype;

import crmdna.practice.Practice.PracticeProp;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProgramTypePropTest {

    @Test
    public void getPracticeIdsTest() {
        ProgramTypeProp programTypeProp = new ProgramTypeProp();

        assertEquals(0, programTypeProp.getPracticeIds().size());

        PracticeProp practiceProp1 = new PracticeProp();
        practiceProp1.practiceId = 1;

        PracticeProp practiceProp2 = new PracticeProp();
        practiceProp2.practiceId = 2;

        programTypeProp.practiceProps.add(practiceProp1);
        programTypeProp.practiceProps.add(practiceProp2);

        Set<Long> practiceIds = programTypeProp.getPracticeIds();
        assertEquals(2, practiceIds.size());

        assertTrue(practiceIds.contains((long) 1));
        assertTrue(practiceIds.contains((long) 2));
    }
}
