package crmdna.payment;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.contact.Contact;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.mail2.MailContent;
import crmdna.payment.Receipt.Purpose;
import crmdna.practice.Practice;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;
import crmdna.registration.Registration;
import crmdna.registration.RegistrationProp;
import crmdna.teacher.Teacher;
import crmdna.user.User;
import crmdna.venue.Venue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ReceiptTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
        new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    Group.GroupProp sgp;
    Practice.PracticeProp shambhavi;
    ProgramTypeProp innerEngineering7Day;
    ProgramProp program;
    Venue.VenueProp giis;
    Teacher.TeacherProp teacher;
    RegistrationProp registration;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + (86400 * 1000)); // tomorrow

        int startDate = calendar.get(Calendar.DAY_OF_MONTH) +
            (calendar.get(Calendar.MONTH) + 1) * 100 +
            calendar.get(Calendar.YEAR) * 10000;

        calendar.setTimeInMillis(System.currentTimeMillis() + (86400 * 1000 * 2)); // day after tomorrow
        int endDate = calendar.get(Calendar.DAY_OF_MONTH) +
            (calendar.get(Calendar.MONTH) + 1) * 100 +
            calendar.get(Calendar.YEAR) * 10000;

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(shambhavi.practiceId);
        innerEngineering7Day = ProgramType.create(client, "Inner Engineering 7 day", practiceIds, User.SUPER_USER);
        giis = Venue.create(client, "GIIS", "GIIS", "GIIS", sgp.groupId, User.SUPER_USER);
        teacher = Teacher.create(client, "", "", "teacher@if.org", sgp.groupId, User.SUPER_USER);
        program = Program.create(client, sgp.groupId, innerEngineering7Day.programTypeId,
            giis.venueId, teacher.teacherId, startDate, endDate, 1, null, 200.0, Utils.Currency.SGD,
            User.SUPER_USER);

        ContactProp contact = new ContactProp();
        contact.firstName = "firstName";
        contact.lastName = "lastName";
        contact.nickName = "nickName";
        contact.gender = Contact.Gender.MALE;
        contact.email = "thulasidhar@gmail.com";
        contact.homePhone = "+124567951";
        contact.mobilePhone = "+124567951";
        contact.officePhone = "+124567951";
        contact.homeAddress.country = "Singapore";
        contact.homeAddress.pincode = "113456";
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());

        registration = Registration.register(client, contact, program.programId, 1, program.fee,
            "offline", null, "http://google.com", "http://google.com", "http://google.com");
        assertEquals(registration.status, Registration.RegistrationStatus.REGISTRATION_COMPLETE);

        try {
            MailContent.create(client, "RESERVED_RECEIPT", sgp.groupId, "RECEIPT",
                Utils.readDataFromURL("https://ishacrmserverdev-t.appspot.com/mailContent/get?client=isha&mailContentId=86"),
                User.SUPER_USER);
        } catch (Exception ex) {

        }
        Group.setContactInfo(client, sgp.groupId, "singapore@ishayoga.org", "Isha Singapore", User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void generateForRegIdTest() {

        try {
            Receipt.generateForRegistration(client, sgp.groupId, registration.registrationId, false);
            ReceiptProp receiptProp = Receipt.getByRegistrationId(client, registration.registrationId);

            assertNotNull(receiptProp);
            assertTrue(receiptProp.ms <= (new Date()).getTime());
            assertEquals(receiptProp.groupId, sgp.groupId);
            assertEquals(receiptProp.registrationId.longValue(), registration.registrationId);
            assertEquals(receiptProp.firstName, registration.firstName);
            assertEquals(receiptProp.lastName, registration.lastName);
            assertEquals(receiptProp.email, registration.email);
            assertEquals(receiptProp.purpose, program.getDetailedName());
            assertEquals(receiptProp.ccy, Utils.Currency.SGD);
            assertEquals(receiptProp.amount, program.fee, 0);
        } catch (APIException ex) {
            System.out.println(ex.toString());
            fail();
        }
    }

    @Test
    public void generateForProgramContribution() {

        String firstName = "FirstName";
        String lastName = "LastName";
        String email = "thulasidhar@gmail.com";
        Purpose purpose = Purpose.VOLUNTEER_CONTRIBUTION;
        double amount = 200.0;

        try {
            Receipt.generateForProgram(client, sgp.groupId, firstName, lastName, email,
                program.programId, purpose, Utils.Currency.SGD, amount);

            Receipt.ReceiptQueryCondition qc = new Receipt.ReceiptQueryCondition();
            qc.groupId = sgp.groupId;
            List<ReceiptProp> receiptProps = Receipt.query(client, qc);
            assertTrue(receiptProps.size() == 1);

            ReceiptProp receiptProp = receiptProps.get(0);
            assertTrue(receiptProp.ms <= (new Date()).getTime());
            assertEquals(receiptProp.groupId, sgp.groupId);
            assertNull(receiptProp.registrationId);
            assertEquals(receiptProp.firstName, firstName);
            assertEquals(receiptProp.lastName, lastName);
            assertEquals(receiptProp.email, email);
            assertEquals(receiptProp.purpose, purpose.toString() + " - " + program.getDetailedName());
            assertEquals(receiptProp.ccy, Utils.Currency.SGD);
            assertEquals(receiptProp.amount, amount, 0);
        } catch (APIException ex) {
            System.out.println(ex.toString());
            fail();
        }
    }

    @Test
    public void generateForAdhocDonation() {

        String firstName = "FirstName";
        String lastName = "LastName";
        String email = "thulasidhar@gmail.com";
        Purpose purpose = Purpose.ADHOC_DONATION;
        double amount = 150.50;
        String adhocReference = "Adhoc Donation";

        try {
            Receipt.generateForAdhocDonation(client, sgp.groupId, firstName, lastName, email,
                adhocReference, purpose, Utils.Currency.SGD, amount);

            Receipt.ReceiptQueryCondition qc = new Receipt.ReceiptQueryCondition();
            qc.groupId = sgp.groupId;
            List<ReceiptProp> receiptProps = Receipt.query(client, qc);
            assertTrue(receiptProps.size() == 1);

            ReceiptProp receiptProp = receiptProps.get(0);
            assertTrue(receiptProp.ms <= (new Date()).getTime());
            assertEquals(receiptProp.groupId, sgp.groupId);
            assertNull(receiptProp.registrationId);
            assertEquals(receiptProp.firstName, firstName);
            assertEquals(receiptProp.lastName, lastName);
            assertEquals(receiptProp.email, email);
            assertEquals(receiptProp.purpose, adhocReference);
            assertEquals(receiptProp.ccy, Utils.Currency.SGD);
            assertEquals(receiptProp.amount, amount, 0);
        } catch (APIException ex) {
            System.out.println(ex.toString());
            fail();
        }
    }

}
