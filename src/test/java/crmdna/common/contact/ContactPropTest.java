package crmdna.common.contact;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class ContactPropTest {

    @Test
    public void getNameTest() {
        ContactProp c = new ContactProp();
        assertEquals(null, c.getName());

        c.firstName = "sathya";
        assertEquals("sathya", c.getName());

        c = new ContactProp();
        c.lastName = "thilakan";
        assertEquals("thilakan", c.getName());

        c = new ContactProp();
        c.firstName = "Sathya";
        c.lastName = "Thilakan";

        assertEquals("Sathya Thilakan", c.getName());
    }

    @Test
    public void getPhoneNosTest() {
        ContactProp c = new ContactProp();
        c.homePhone = "6565387800";
        c.mobilePhone = "6593839390";

        assertEquals("6593839390 6565387800", c.getPhoneNos());
    }
}
