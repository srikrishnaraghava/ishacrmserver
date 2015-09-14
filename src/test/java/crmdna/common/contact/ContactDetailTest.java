package crmdna.common.contact;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ContactDetailTest {

    @Test
    public void ensureValidTest() {
        assertTrue(false);
    }

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
        assertTrue(false);
    }

    @Test
    public void ensureFirstNameAndValidEmailSpecified() {
        assertTrue(false);
    }

    @Test
    public void getContactDetailFromMap() {
        assertTrue(false);
    }

    @Test
    public void getContactDetailsFromListOfMap() {
        assertTrue(false);
    }

    @Test
    public void validateTest() {
        assertTrue(false);
    }
}
