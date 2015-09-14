package crmdna.participant;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParticipantPropTest {

    @Test
    public void compareToTest() {
        ParticipantProp sathya = new ParticipantProp();
        sathya.participantId = 1;
        sathya.contactDetail.firstName = "Sathya";

        ParticipantProp thulasi = new ParticipantProp();
        thulasi.participantId = 2;
        thulasi.contactDetail.firstName = "Thulasi";

        ParticipantProp giri = new ParticipantProp();
        giri.participantId = 3;
        giri.contactDetail.firstName = "Giridhar";

        ParticipantProp noname = new ParticipantProp();
        noname.participantId = 4;
        //no first name specified

        List<ParticipantProp> participantProps = new ArrayList<>();
        participantProps.add(sathya);
        participantProps.add(thulasi);
        participantProps.add(giri);
        participantProps.add(noname);

        Collections.sort(participantProps);
        //should be sorted in ascending order

        assertEquals(giri.participantId, participantProps.get(0).participantId);
        assertEquals(sathya.participantId, participantProps.get(1).participantId);
        assertEquals(thulasi.participantId, participantProps.get(2).participantId);
        assertEquals(noname.participantId, participantProps.get(3).participantId);
    }
}
