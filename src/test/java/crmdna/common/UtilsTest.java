package crmdna.common;

import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class UtilsTest {
    @Test
    public void getQSTagsTest_old() {

        // Set<String> qsTags = Utils.getQSTags("sathyanarayanant@gmail.com",
        // "sathyanarayanan", "thilakan", "98361844", null, "65227030");
        Set<String> qsTags =
                Utils
                        .getQSTags_old("sathyanarayanant@gmail.com", "Sathyanarayanan", "thilakan", null, null);

        Set<String> expected = new TreeSet<>();
        expected.add("sat");
        expected.add("ath");
        expected.add("thy");
        expected.add("hya");
        expected.add("yan");
        expected.add("ana");
        expected.add("nar");
        expected.add("ara");
        expected.add("ray");
        expected.add("aya");
        expected.add("yan");
        expected.add("ana");
        expected.add("nan");
        expected.add("ant");
        expected.add("nt@");
        expected.add("t@g");
        expected.add("@gm");
        expected.add("gma");
        expected.add("mai");
        expected.add("ail");
        expected.add("il.");
        expected.add("l.c");
        expected.add(".co");
        expected.add("com");

        // thilakan
        expected.add("thi");
        expected.add("hil");
        expected.add("ila");
        expected.add("lak");
        expected.add("aka");
        expected.add("kan");

        assertTrue(qsTags.containsAll(expected));
        assertEquals(expected.size(), qsTags.size());
    }

    @Test
    public void getQSTags2Test() {

        // Set<String> qsTags = Utils.getQSTags("sathyanarayanant@gmail.com",
        // "sathyanarayanan", "thilakan", "98361844", null, "65227030");
        Set<String> qsTags =
                Utils.getQSTags("sathyanarayanant@gmail.com", "Sathyanarayanan", "thilakan", null, null);

        Set<String> expected = new TreeSet<>();
        expected.add("sat");
        expected.add("ath");
        expected.add("thy");
        expected.add("hya");
        expected.add("yan");
        expected.add("ana");
        expected.add("nar");
        expected.add("ara");
        expected.add("ray");
        expected.add("aya");
        expected.add("yan");
        expected.add("ana");
        expected.add("nan");
        expected.add("ant");
        expected.add("nt@");
        expected.add("t@g");
        expected.add("@gm");
        expected.add("gma");
        expected.add("mai");
        expected.add("ail");
        expected.add("il.");
        expected.add("l.c");
        expected.add(".co");
        expected.add("com");

        // thilakan
        expected.add("thi");
        expected.add("hil");
        expected.add("ila");
        expected.add("lak");
        expected.add("aka");
        expected.add("kan");

        assertTrue(qsTags.containsAll(expected));
        assertEquals(expected.size(), qsTags.size());

        qsTags = Utils.getQSTags("Giridhar       	Nayak", null, null); // has
        // spaces
        // and
        // tabs

        Set<String> qsTags2 = Utils.getQSTags("Giridhar", " Nayak", null, null);
        assertTrue(qsTags.containsAll(qsTags2));
        assertEquals(qsTags.size(), qsTags.size());

        // should convert to lower case
        qsTags2 = Utils.getQSTags("giridhar", " nayak   ", null, null);
        assertTrue(qsTags.containsAll(qsTags2));
        assertEquals(qsTags.size(), qsTags.size());

        expected.clear();
        expected.add("gir");
        expected.add("iri");
        expected.add("rid");
        expected.add("idh");
        expected.add("dha");
        expected.add("har");
        expected.add("nay");
        expected.add("aya");
        expected.add("yak");

        assertTrue(qsTags.containsAll(expected));
        assertEquals(qsTags.size(), expected.size());

        // less than 3 char
        qsTags = Utils.getQSTags("s a th ya");
        assertEquals(0, qsTags.size());

        qsTags = Utils.getQSTags("s", " a", "th 	", "ya");
        assertEquals(0, qsTags.size());

        // can pass nulls
        qsTags = Utils.getQSTags(null, null, null);
        assertEquals(0, qsTags.size());

        qsTags = Utils.getQSTags(null, null, null, "sathya");
        expected.clear();
        expected.add("sat");
        expected.add("ath");
        expected.add("thy");
        expected.add("hya");

        assertTrue(qsTags.containsAll(expected));
        assertEquals(qsTags.size(), expected.size());
    }

    @Test
    public void isPresentInListCaseInsensitiveTest() {
        assertEquals(false, Utils.isPresentInListCaseInsensitive(null, null));

        assertEquals(false, Utils.isPresentInListCaseInsensitive(new ArrayList<String>(), null));

        assertEquals(false, Utils.isPresentInListCaseInsensitive(new ArrayList<String>(), ""));

        List<String> list = new ArrayList<String>();
        list.add("sathya");
        list.add("sowmya");
        list.add("balaji");

        assertEquals(true, Utils.isPresentInListCaseInsensitive(list, "sAthYa"));
        assertEquals(true, Utils.isPresentInListCaseInsensitive(list, "balaji"));
        assertEquals(true, Utils.isPresentInListCaseInsensitive(list, "SowmYa"));

        assertEquals(false, Utils.isPresentInListCaseInsensitive(list, "thilakan"));

    }

    @Test
    public void throwIncorrectSpecificationExceptionTest() {
        try {
            Utils.throwIncorrectSpecException("message 1");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("exception should be incorrect specification exception",
                    Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
            assertEquals("message should be correct", "message 1", ex.userFriendlyMessage);
        }
    }

    @Test
    public void throwNotFoundExceptionTest() {
        try {
            Utils.throwNotFoundException("message 1");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("exception should be incorrect specification exception",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
            assertEquals("message should be correct", "message 1", ex.userFriendlyMessage);
        }
    }

    @Test
    public void ensureValidPhoneNumberTest() {

        // cannot be null
        try {
            Utils.ensureValidPhoneNumber(null);
            assertTrue(false);
        } catch (APIException ex) {
            assertTrue(ex.statusCode == Status.ERROR_RESOURCE_INCORRECT);
        }

        // cannot be empty string
        try {
            Utils.ensureValidPhoneNumber("");
            assertTrue(false);
        } catch (APIException ex) {
            assertTrue(ex.statusCode == Status.ERROR_RESOURCE_INCORRECT);
        }

        // should start with +
        try {
            Utils.ensureValidPhoneNumber("914422590181");
            assertTrue(false);
        } catch (APIException ex) {
            assertTrue(ex.statusCode == Status.ERROR_RESOURCE_INCORRECT);
        }

        // valid singapore number
        Utils.ensureValidPhoneNumber("+6598361844"); // no exception
        Utils.ensureValidPhoneNumber("+6593232152"); // no exception
        Utils.ensureValidPhoneNumber("+6565227030"); // no exception

        // india phone numbers
        Utils.ensureValidPhoneNumber("+914422593926"); // no exception
        Utils.ensureValidPhoneNumber("+919841283881"); // no exception
        Utils.ensureValidPhoneNumber("+919819532634"); // no exception

        // australia phone numbers
        Utils.ensureValidPhoneNumber("+61469218170"); // no exception

        // US phone number
        Utils.ensureValidPhoneNumber("+19198894985"); // no exception
    }

    @Test
    public void ensureValidUrlTest() {

        Utils.ensureValidUrl("http://www.google.com");

        try {
            Utils.ensureValidUrl("dummy");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void ensureValidEmailTest() {

        // cannot be null
        try {
            Utils.ensureValidEmail(null);
            assertTrue(false);
        } catch (APIException ex) {
            assertTrue(ex.statusCode == Status.ERROR_RESOURCE_INCORRECT);
        }

        // cannot be empty string
        try {
            Utils.ensureValidEmail("");
            assertTrue(false);
        } catch (APIException ex) {
            assertTrue(ex.statusCode == Status.ERROR_RESOURCE_INCORRECT);
        }

        // cannot have special char
        try {
            Utils.ensureValidEmail("@@@sathya.com");
            assertTrue(false);
        } catch (APIException ex) {
            assertTrue(ex.statusCode == Status.ERROR_RESOURCE_INCORRECT);
        }

        // valid email addresses
        Utils.ensureValidEmail("123@sathya.com");
        Utils.ensureValidEmail("sathya.t@ishafoundation.org");
        Utils.ensureValidEmail("ramya.c@ishafoundation.org");
    }

    @Test
    public void closeEnoughTest() {
        assertFalse(Utils.closeEnough(null, null));

        assertTrue(Utils.closeEnough("sathya", "sathya"));
        assertTrue(Utils.closeEnough("sathya", "sathyan"));
        assertTrue(Utils.closeEnough("sathya", "sathyanarayanan"));
        assertTrue(Utils.closeEnough("radhika", "radikaa"));
        assertTrue(Utils.closeEnough("thulasi", "tulasi"));

        assertFalse(Utils.closeEnough("sathya", "sowmya"));
        assertFalse(Utils.closeEnough("paramesh", "giridhar"));
        assertFalse(Utils.closeEnough("giridhar", "raadhika"));
        assertFalse(Utils.closeEnough("thulasidhar", "giridhar"));
        assertFalse(Utils.closeEnough("ramesh", "suresh"));
        assertFalse(Utils.closeEnough("sathya", "syamala"));

        String s1 = "Sathya narayanan";
        String s2 = "sathya Thilakan";

        // ensure s1 and s2 are not changed
        assertTrue(Utils.closeEnough(s1, s2));
        assertEquals("Sathya narayanan", s1);
        assertEquals("sathya Thilakan", s2);

        assertTrue(Utils.closeEnough("sathya thilakan", "thilakan sathya"));
    }

    @Test
    public void getFullNameTest() {
        assertEquals("Sathya Thilakan", Utils.getFullName("Sathya", "Thilakan"));
        assertEquals("Sathya", Utils.getFullName("Sathya", null));
        assertEquals("Thilakan", Utils.getFullName(null, "Thilakan"));
        assertEquals(null, Utils.getFullName(null, null));
    }

    @Test
    public void ensureNonNegativeTest() {
        Utils.ensureNonNegative(0); // no exception
        Utils.ensureNonNegative(10.09384); // no exception

        try {
            Utils.ensureNonNegative(-10);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, e.statusCode);
        }
    }

    @Test
    public void asCurrencyStringTest() {
        assertEquals("150.00", Utils.asCurrencyString(150));
        assertEquals("150.55", Utils.asCurrencyString(150.546));
        assertEquals(".50", Utils.asCurrencyString(0.501));
    }

    @Test
    public void safeParseAsLongTest() {
        assertEquals(100, Utils.safeParseAsLong("100"));
        assertEquals(-100, Utils.safeParseAsLong("-100"));
        assertEquals(0, Utils.safeParseAsLong("0"));

        try {
            Utils.safeParseAsLong("abc");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        try {
            Utils.safeParseAsLong("100.23");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void safeParseAsIntTest() {
        assertEquals(100, Utils.safeParseAsInt("100"));
        assertEquals(-100, Utils.safeParseAsInt("-100"));
        assertEquals(0, Utils.safeParseAsInt("0"));

        try {
            Utils.safeParseAsInt("abc");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        try {
            Utils.safeParseAsInt("100.23");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void safeParseAsDouble() {
        assertTrue(100 == Utils.safeParseAsDouble("100"));
        assertTrue(100 == Utils.safeParseAsDouble("100.00"));
        assertTrue(-100 == Utils.safeParseAsDouble("-100"));
        assertTrue(-100.098 == Utils.safeParseAsDouble("-100.098"));
        assertTrue(0 == Utils.safeParseAsDouble("0"));

        try {
            Utils.safeParseAsDouble("abc");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void urlEncodeTest() {

        String url = "http://www.google.com/test?key=value";
        String expectedAfterEncoding = "http%3A%2F%2Fwww.google.com%2Ftest%3Fkey%3Dvalue";

        assertEquals(expectedAfterEncoding, Utils.urlEncode(url));
    }

    @Test
    public void getUrlTest() {

        String baseUrl = "http://www.google.com";

        Map<String, Object> map = new HashMap<>();
        map.put("firstName", "sathya");
        map.put("programType", "Surya Kriya");
        map.put("program Id", new Long(6));
        map.put("alreadyRegistered", true);

        String url = Utils.getUrl(baseUrl, map);
        assertEquals(
                "http://www.google.com?alreadyRegistered=true&firstName=sathya&program%20Id=6&programType=Surya%20Kriya&",
                url);
    }

    @Test
    public void ensureNotNullOrEmptyTest() {

        Utils.ensureNotNullOrEmpty("ishacrm", "no exception");

        try {
            Utils.ensureNotNullOrEmpty("", "message");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
            assertEquals("message", ex.userFriendlyMessage);
        }

        try {
            Utils.ensureNotNullOrEmpty(null, "message");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
            assertEquals("message", ex.userFriendlyMessage);
        }
    }

    @Test
    public void ensureNonZeroTest() {

        Utils.ensureNonZero(100, "no exception");

        try {
            Utils.ensureNonZero(0, "message");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
            assertEquals("message", ex.userFriendlyMessage);
        }
    }

    @Test
    public void csvEncodeTest() {

        assertEquals("", Utils.csvEncode(null));
        assertEquals("sathya", Utils.csvEncode("sathya"));
        assertEquals("\"block 871b, 10-92\"", Utils.csvEncode("block 871b, 10-92"));

        //double quote should be stripped
        assertEquals("Raj vuppu", Utils.csvEncode("\"Raj vuppu"));
    }

    @Test
    public void sanitizePhoneNoTest() {
        String phoneNo = Utils.sanitizePhoneNo("006593232152", "sgp");
        assertEquals("+6593232152", phoneNo);

        assertEquals("+6593232152", Utils.sanitizePhoneNo("93232152", "sgp"));

        assertEquals("+6593232152", Utils.sanitizePhoneNo("06593232152", "sgp"));

        assertEquals("+6593232152", Utils.sanitizePhoneNo("065(932)32152", "sgp"));

        assertEquals("+971507000000", Utils.sanitizePhoneNo("971507000000", "sgp"));
    }

    @Test
    public void isDifferentCaseInsensitive() {
        assertEquals(true, Utils.isDifferentCaseInsensitive("sathya", null));
        assertEquals(true, Utils.isDifferentCaseInsensitive(null, null));
        assertEquals(false, Utils.isDifferentCaseInsensitive("sathya", "Sathya"));
        assertEquals(true, Utils.isDifferentCaseInsensitive("sathya", "Sathya narayanan"));

        // ensure inputs do not change
        String s1 = "Sowmya";
        String s2 = "Sowmya Ramakrishnan";
        assertEquals(true, Utils.isDifferentCaseInsensitive(s1, s2));
        assertEquals("Sowmya", s1);
        assertEquals("Sowmya Ramakrishnan", s2);
    }

    @Test
    public void getWeightedAvgTest() {
        List<Double> prices = new ArrayList<Double>();
        List<Double> quantities = new ArrayList<Double>();

        prices.add(100.0);
        quantities.add(1.0);

        double avg = Utils.getWeightedAvg(prices, quantities);
        assertTrue(100.0 == avg);

        prices.add(120.0);
        quantities.add(1.0);

        avg = Utils.getWeightedAvg(prices, quantities);
        assertTrue(110.0 == avg);

        prices.add(110.0);
        quantities.add(5.0);
        avg = Utils.getWeightedAvg(prices, quantities);
        assertTrue(110.0 == avg);
    }

    @Test
    public void getHrefsTest() {
        String html =
                "dummy dummy <a href=\"http://www.google.com\">"
                        + "dummy dummy <a href=\"http://www.yahoo.com\">";

        Set<String> hrefs = Utils.getHrefs(html);
        assertEquals(2, hrefs.size());
        assertTrue(hrefs.contains("http://www.google.com"));
        assertTrue(hrefs.contains("http://www.yahoo.com"));

        hrefs = Utils.getHrefs("");
        assertEquals(0, hrefs.size());
    }

    @Test
    public void getRandomAlphaNumericTest() {
        try {
            Utils.getRandomAlphaNumericString(0);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        String s = Utils.getRandomAlphaNumericString(6);
        assertEquals(6, s.length());
        assertTrue(!s.matches("^.*[^a-zA-Z0-9].*$"));

        s = Utils.getRandomAlphaNumericString(10);
        assertEquals(10, s.length());
        assertTrue(!s.matches("^.*[^a-zA-Z0-9].*$"));
    }

    @Test
    public void testThatToHexStringWorks() {
        byte[] bytes = new byte[]{1, 2, 3, 4};
        assertEquals("01020304", Utils.toHexStr(bytes));
    }

    @Test
    public void testThatToHexStringReturnsNullForNullInput() {
        assertNull(Utils.toHexStr(null));
    }

    @Test
    public void testThatToHexStringWorksForNegativeBytes() {
        byte[] bytes = new byte[]{-1, -2, -3, -4};
        assertEquals("fffefdfc", Utils.toHexStr(bytes));
    }

    @Test
    public void testThatToByteArrayWorks() {
        byte[] bytes = Utils.toByteArray("01020304");
        assertArrayEquals(new byte[]{1, 2, 3, 4}, bytes);
    }

    @Test
    public void testThatToByteArrayReturnsNullForNullInput() {
        assertNull(Utils.toByteArray(null));
    }

    @Test
    public void testThatToByteArrayWorksForNegativeBytes() {
        byte[] bytes = Utils.toByteArray("fffefdfc");
        assertArrayEquals(new byte[]{-1, -2, -3, -4}, bytes);
    }

    @Test
    public void testThatToByteArrayThrowsExceptionForOddInputLen() {
        try {
            Utils.toByteArray("fffefdfca");
            fail("Expected an exception to be thrown.");
        } catch (APIException e) {
            assertEquals(Status.ERROR_INVALID_INPUT, e.statusCode);
            // Message better be descriptive with the expected length.
            assertTrue(e.userFriendlyMessage.contains("even"));
        }
    }
}
