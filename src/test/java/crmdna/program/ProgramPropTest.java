package crmdna.program;

import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group.GroupProp;
import crmdna.programtype.ProgramTypeProp;
import crmdna.venue.Venue.VenueProp;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProgramPropTest {
    @Test
    public void getSessionsTest() {

        ProgramProp programProp = new ProgramProp();
        programProp.programId = 123;
        programProp.groupProp = new GroupProp();
        programProp.groupProp.displayName = "Singapore";
        programProp.programTypeProp = new ProgramTypeProp();
        programProp.programTypeProp.displayName = "IshaKriya";
        programProp.venueProp = new VenueProp();
        programProp.venueProp.displayName = "Woodlands CC";
        programProp.numBatches = 2;

        List<SessionProp> sessionProps = programProp.getSessions(20140309);
        assertEquals(2, sessionProps.size());
        assertEquals(1, sessionProps.get(0).batchNo);
        assertEquals("Singapore", sessionProps.get(0).center);
        assertEquals(123, sessionProps.get(0).programId);
        assertEquals(20140309, sessionProps.get(0).dateYYYYMMDD);
        assertEquals("IshaKriya", sessionProps.get(0).programType);
        assertEquals("9 Mar B1 IshaKriya @ Woodlands ~", sessionProps.get(0).title);

        assertEquals(2, sessionProps.get(1).batchNo);
        assertEquals("Singapore", sessionProps.get(1).center);
        assertEquals(123, sessionProps.get(1).programId);
        assertEquals(20140309, sessionProps.get(1).dateYYYYMMDD);
        assertEquals("IshaKriya", sessionProps.get(1).programType);
        assertEquals("9 Mar B2 IshaKriya @ Woodlands ~", sessionProps.get(1).title);
    }

    @Test
    public void getNumSessionsTest() {
        //1 day program
        ProgramProp programProp = new ProgramProp();
        programProp.startYYYYMMDD = 20150925;
        programProp.endYYYYMMDD = 20150925;
        assertEquals(1, programProp.getNumSessions());

        //2 day program like shambhavi
        programProp.startYYYYMMDD = 20150930;
        programProp.endYYYYMMDD = 20151001;
        assertEquals(2, programProp.getNumSessions());
    }

    @Test
    public void ensureValidSessionDateTest() {
        ProgramProp programProp = new ProgramProp();
        programProp.startYYYYMMDD = 20140501;
        programProp.endYYYYMMDD = 20140521;

        programProp.ensureValidSessionDate(20140501); // no exception
        programProp.ensureValidSessionDate(20140521); // no exception
        programProp.ensureValidSessionDate(20140515); // no exception

        try {
            programProp.ensureValidSessionDate(20140523);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        try {
            programProp.ensureValidSessionDate(20141523); // invalid date
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // check for a 1 day program
        programProp.startYYYYMMDD = 20140501;
        programProp.endYYYYMMDD = 20140501;

        programProp.ensureValidSessionDate(20140501); // no exception

        try {
            programProp.ensureValidSessionDate(20140330); // 1 day before
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        try {
            programProp.ensureValidSessionDate(20140502); // 1 day after
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void asMapTest() {
        ProgramProp programProp = new ProgramProp();
        programProp.programTypeProp = new ProgramTypeProp();
        programProp.programTypeProp.displayName = "Shambhavi (2 day)";

        programProp.startYYYYMMDD = 20150925;
        programProp.endYYYYMMDD = 20150925;

        programProp.venueProp = new VenueProp();
        programProp.venueProp.displayName = "GIIS";
        programProp.venueProp.address = "GIIS queenstown";

        programProp.groupProp = new GroupProp();
        programProp.groupProp.displayName = "Isha Singapore";

        Map<String, Object> map = programProp.asMap();
        assertEquals(map.get("programType"), "Shambhavi (2 day)");
        assertEquals("20150925", map.get("startDateYYYYMMDD"));
        assertEquals("20150925", map.get("endDateYYYYMMDD"));
        assertEquals("GIIS", map.get("venue"));
        assertEquals("GIIS queenstown", map.get("venueFullAddress"));
        assertEquals("Isha Singapore", map.get("groupName"));
    }
}
