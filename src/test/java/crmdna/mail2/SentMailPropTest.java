package crmdna.mail2;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.user.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SentMailPropTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
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
        System.clearProperty(Mail.SYSTEM_PROPERTY_SUPPRESS_EMAIL);
    }

    @Test
    public void populateDependentsTest() {

        long mailContentId1 = MailContent
                .create(client,
                        "name1",
                        sgp.groupId,
                        "subject1",
                        "Dear *|FNAME|* *|LNAME|* we cordially invite. regards, IshaSingapore",
                        User.SUPER_USER).mailContentId;

        long mailContentId2 = MailContent
                .create(client,
                        "name2",
                        sgp.groupId,
                        "subject2",
                        "Dear *|FNAME|* *|LNAME|* we cordially invite2. regards, IshaSingapore",
                        User.SUPER_USER).mailContentId;

        SentMailProp prop1 = new SentMailProp();
        prop1.mailContentId = mailContentId1;

        SentMailProp prop2 = new SentMailProp();
        prop2.mailContentId = mailContentId2;

        SentMailProp.populateDependents(client, Utils.getList(prop1, prop2));

        assertEquals("subject1", prop1.subject);
        assertEquals("subject2", prop2.subject);

    }
}
