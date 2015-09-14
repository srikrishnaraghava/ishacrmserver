package crmdna.datamigration;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.group.GroupEntity;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.practice.PracticeEntity;
import crmdna.user.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static crmdna.common.OfyService.ofy;
import static org.junit.Assert.assertEquals;

public class DataMigrationTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    String client;
    String invalidClient = "invalid";

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        client = "isha";
        Client.create(client);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void copyDataToAnotherClientTest() {
        String sourceClient = "source";
        String targetClient = "target";

        Client.create(sourceClient);
        Client.create(targetClient);

        GroupProp groupProp = Group.create(sourceClient, "Group1", User.SUPER_USER);
        assertEquals(1, groupProp.groupId);
        groupProp = Group.create(sourceClient, "Group2", User.SUPER_USER);
        assertEquals(2, groupProp.groupId);

        PracticeProp practiceProp = Practice.create(sourceClient, "Practice1", User.SUPER_USER);
        assertEquals(1, practiceProp.practiceId);
        practiceProp = Practice.create(sourceClient, "Practice2", User.SUPER_USER);
        assertEquals(2, practiceProp.practiceId);

        DataMigration.copyAllEntitiesToAnotherClient(sourceClient, targetClient, User.SUPER_USER);

        List<GroupEntity> groupEntities = ofy(targetClient).load().type(GroupEntity.class).list();
        assertEquals(2, groupEntities.size());

        Collections.sort(groupEntities, new Comparator<GroupEntity>() {

            @Override
            public int compare(GroupEntity o1, GroupEntity o2) {
                // TODO Auto-generated method stub
                return new Long(o1.toProp().groupId).compareTo(new Long(o2.toProp().groupId));
            }
        });

        assertEquals(1, groupEntities.get(0).toProp().groupId);
        assertEquals("Group1", groupEntities.get(0).toProp().displayName);

        assertEquals(2, groupEntities.get(1).toProp().groupId);
        assertEquals("Group2", groupEntities.get(1).toProp().displayName);

        List<PracticeEntity> practiceEntities = ofy(targetClient).load().type(PracticeEntity.class).list();
        assertEquals(2, practiceEntities.size());

        Collections.sort(practiceEntities, new Comparator<PracticeEntity>() {

            @Override
            public int compare(PracticeEntity o1, PracticeEntity o2) {
                // TODO Auto-generated method stub
                return new Long(o1.toProp().practiceId).compareTo(new Long(o2.toProp().practiceId));
            }
        });

        assertEquals(1, practiceEntities.get(0).toProp().practiceId);
        assertEquals("Practice1", practiceEntities.get(0).toProp().displayName);

        assertEquals(2, practiceEntities.get(1).toProp().practiceId);
        assertEquals("Practice2", practiceEntities.get(1).toProp().displayName);

        //source is not deleted
        int count = ofy(sourceClient).load().type(GroupEntity.class).count();
        assertEquals(2, count);

        count = ofy(sourceClient).load().type(PracticeEntity.class).count();
        assertEquals(2, count);
    }
}
