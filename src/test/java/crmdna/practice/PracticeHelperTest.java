package crmdna.practice;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.ICode;
import crmdna.member.MemberProp;
import crmdna.practice.Practice.PracticeProp;
import crmdna.user.User;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.TestUtil.ensureResourceNotFoundException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PracticeHelperTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";

    PracticeProp ishaKriya;
    PracticeProp shambhavi;
    PracticeProp shoonya;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);
        assertEquals(1, ishaKriya.practiceId);

        shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER);
        assertEquals(2, shambhavi.practiceId);

        shoonya = Practice.create(client, "Shoonya", User.SUPER_USER);
        assertEquals(3, shoonya.practiceId);
    }

    @Test
    public void populateTest() {
        MemberProp memberProp1 = new MemberProp();
        memberProp1.practiceIds.add(ishaKriya.practiceId);
        memberProp1.practiceIds.add(shambhavi.practiceId);

        MemberProp memberProp2 = new MemberProp();
        memberProp2.practiceIds.add(ishaKriya.practiceId);
        memberProp2.practiceIds.add(shambhavi.practiceId);
        memberProp2.practiceIds.add(shoonya.practiceId);
        memberProp2.practiceIds.add((long) 100); // non existent practice

        final List<MemberProp> memberProps = new ArrayList<>();
        memberProps.add(memberProp1);
        memberProps.add(memberProp2);

        PracticeHelper.populateName(client, memberProps);
        assertEquals(2, memberProp1.practices.size());
        assertTrue(memberProp1.practices.contains("Isha Kriya"));
        assertTrue(memberProp1.practices.contains("Shambhavi"));

        assertEquals(3, memberProp2.practices.size());
        assertTrue(memberProp2.practices.contains("Isha Kriya"));
        assertTrue(memberProp2.practices.contains("Shambhavi"));
        assertTrue(memberProp2.practices.contains("Shoonya"));

        // client should be valid
        ensureResourceNotFoundException(new ICode() {

            @Override
            public void run() {
                PracticeHelper.populateName(invalidClient, memberProps);
            }
        });
    }
}
