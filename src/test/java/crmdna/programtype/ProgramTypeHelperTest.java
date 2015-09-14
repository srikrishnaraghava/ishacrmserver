package crmdna.programtype;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.ICode;
import crmdna.member.MemberProgramProp;
import crmdna.user.User;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static crmdna.common.TestUtil.ensureResourceNotFoundException;
import static org.junit.Assert.assertEquals;

public class ProgramTypeHelperTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";

    ProgramTypeProp ishaKriya1hour;
    ProgramTypeProp shambhavi2day;
    ProgramTypeProp shoonya4day;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        ishaKriya1hour =
                ProgramType.create(client, "Isha Kriya (1 hour)", new HashSet<Long>(), User.SUPER_USER);
        assertEquals(1, ishaKriya1hour.programTypeId);

        shambhavi2day =
                ProgramType.create(client, "Shambhavi (2 day)", new HashSet<Long>(), User.SUPER_USER);
        assertEquals(2, shambhavi2day.programTypeId);

        shoonya4day =
                ProgramType.create(client, "Shoonya (4 day)", new HashSet<Long>(), User.SUPER_USER);
        assertEquals(3, shoonya4day.programTypeId);
    }

    @Test
    public void populateTest() {
        MemberProgramProp memberProgramProp1 = new MemberProgramProp();
        memberProgramProp1.programTypeId = ishaKriya1hour.programTypeId;

        MemberProgramProp memberProgramProp2 = new MemberProgramProp();
        memberProgramProp2.programTypeId = shambhavi2day.programTypeId;

        MemberProgramProp memberProgramProp3 = new MemberProgramProp();
        memberProgramProp3.programTypeId = 100; // non existent

        final List<MemberProgramProp> memberProgramProps = new ArrayList<>();
        memberProgramProps.add(memberProgramProp1);
        memberProgramProps.add(memberProgramProp2);
        memberProgramProps.add(memberProgramProp3);

        ProgramTypeHelper.populateName(client, memberProgramProps);

        assertEquals("program type populated", "Isha Kriya (1 hour)", memberProgramProp1.programType);
        assertEquals("program type populated", "Shambhavi (2 day)", memberProgramProp2.programType);
        assertEquals("works if programTypeId is invalid", null, memberProgramProp3.programType);

        // client should be valid
        ensureResourceNotFoundException(new ICode() {

            @Override
            public void run() {
                ProgramTypeHelper.populateName(invalidClient, memberProgramProps);
            }
        });
    }
}
