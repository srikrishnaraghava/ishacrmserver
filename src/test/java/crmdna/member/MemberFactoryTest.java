package crmdna.member;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.ICode;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.member.MemberEntity.MemberFactory;
import crmdna.user.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.TestUtil.ensureResourceIncorrectException;
import static crmdna.common.TestUtil.ensureResourceNotFoundException;
import static org.junit.Assert.assertEquals;

public class MemberFactoryTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private GroupProp sgp;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void createTest() {

        final ContactProp contact = new ContactProp();
        contact.email = "sathya.t@ishafoundation.org";
        contact.firstName = "sathya";
        contact.asOfyyyymmdd = 20140618;

        // client should be valid
        ensureResourceNotFoundException(new ICode() {
            @Override
            public void run() {
                MemberFactory.create(invalidClient, 1);
            }

            ;
        });

        // create 1
        MemberEntity memberEntity = MemberFactory.create(client, 1).get(0);
        assertEquals(1, memberEntity.memberId); // memberId should be 1

        // create many
        final List<ContactProp> contacts = new ArrayList<>();
        contacts.add(contact);

        ContactProp contact2 = new ContactProp();
        contact2.asOfyyyymmdd = 20140618;
        contacts.add(contact2);


        List<MemberEntity> memberEntities = MemberFactory.create(client, contacts.size());
        assertEquals(2, memberEntities.size());
        assertEquals(2, memberEntities.get(0).memberId);
        assertEquals(3, memberEntities.get(1).memberId);

        // create 1000
        contacts.clear();
        for (int i = 0; i < 1000; i++) {
            ContactProp c = new ContactProp();
            c.asOfyyyymmdd = 20140618;
            contacts.add(c);
        }
        memberEntities = MemberFactory.create(client, contacts.size());
        assertEquals(1000, memberEntities.size());
        assertEquals(1003, memberEntities.get(999).memberId);

        for (int i = 0; i < 20000; i++) {
            ContactProp c = new ContactProp();
            c.asOfyyyymmdd = 20140618;
            contacts.add(c);
        }

        // cannot create more than 10000 in one shot. (safety feature)
        ensureResourceIncorrectException(new ICode() {

            @Override
            public void run() {
                MemberFactory.create(client, contacts.size());
            }
        });
    }
}
