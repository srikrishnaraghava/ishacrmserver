package crmdna.inventory;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.hr.Department;
import crmdna.hr.DepartmentProp;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IshaInventoryItemTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String validUser = "valid@login.com";
    private final String userWithPermission = "withpermission@login.com";
    InventoryItemTypeProp book;
    InventoryItemTypeProp rudraksh;
    DepartmentProp nandiFoods;
    private GroupProp sgp;
    private GroupProp kl;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = Group.create(client, "KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);

        book = InventoryItemType.create(client, "Book", User.SUPER_USER);
        assertEquals(1, book.inventoryItemTypeId);

        rudraksh = InventoryItemType
                .create(client, "Rudraksh", User.SUPER_USER);
        assertEquals(2, rudraksh.inventoryItemTypeId);

        User.create(client, validUser, sgp.groupId, User.SUPER_USER);
        assertEquals(1,
                UserCore.safeGet(client, validUser).toProp(client).userId);

        User.create(client, userWithPermission, sgp.groupId, User.SUPER_USER);
        assertEquals(
                2,
                UserCore.safeGet(client, userWithPermission).toProp(client).userId);

        User.addGroupLevelPrivilege(client, sgp.groupId, userWithPermission,
                GroupLevelPrivilege.UPDATE_INVENTORY_ITEM, User.SUPER_USER);
        User.addGroupLevelPrivilege(client, sgp.groupId, userWithPermission,
                GroupLevelPrivilege.UPDATE_INVENTORY_QUANTITY, User.SUPER_USER);

        nandiFoods = Department.create(client, "Nandi Foods", User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void toDo() {
        assertTrue(false);
    }
}
