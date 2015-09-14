package crmdna.program;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SessionPropTest {
    @Test
    public void populateTitleTest() {
        SessionProp sessionProp = new SessionProp();
        sessionProp.batchNo = 1;
        sessionProp.dateYYYYMMDD = 20140309;
        sessionProp.programType = "Kids Yoga";
        sessionProp.venue = "GIIS Queenstown";

        sessionProp.populateTitle(2);
        assertEquals("9 Mar B1 Kids Yoga @ GIIS Queen~", sessionProp.title);

        sessionProp.populateTitle(1);
        assertEquals("9 Mar Kids Yoga @ GIIS Queen~", sessionProp.title);

        sessionProp.programType = "Yoga for Children";
        sessionProp.populateTitle(2);
        assertEquals("9 Mar B1 Yoga for C~ @ GIIS Queen~", sessionProp.title);
    }
}
