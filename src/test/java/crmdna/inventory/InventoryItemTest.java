package crmdna.inventory;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.ICode;
import crmdna.common.UnitUtils.PhysicalQuantity;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.hr.Department;
import crmdna.hr.DepartmentProp;
import crmdna.inventory.InventoryItemCore.CheckInOrOut;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static crmdna.common.TestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InventoryItemTest {
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
    public void createTest() {
        InventoryItemProp forestFlower = InventoryItem.create(client,
                sgp.groupId, book.inventoryItemTypeId, "Forest Flower",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);

        assertEquals(1, forestFlower.inventoryItemId);

        ObjectifyFilter.complete();

        forestFlower = InventoryItem.safeGet(client,
                forestFlower.inventoryItemId).toProp();
        assertEquals(1, forestFlower.inventoryItemId);
        assertEquals(sgp.groupId, forestFlower.groupId);
        assertEquals(book.inventoryItemTypeId, forestFlower.inventoryItemTypeId);
        assertEquals("Forest Flower", forestFlower.displayName);

        // name should be populated correctly in the entity
        assertEquals(
                "forestflower",
                InventoryItem.safeGet(client, forestFlower.inventoryItemId).name);

        assertEquals(PhysicalQuantity.NUMBER, forestFlower.physicalQuantity);
        assertEquals(ReportingUnit.NUMBER, forestFlower.reportingUnit);

        // client cannot be invalid
        ensureResourceNotFoundException(new ICode() {
            @Override
            public void run() {
                InventoryItem.create("invalid", sgp.groupId,
                        book.inventoryItemTypeId, "joy 24/7",
                        PhysicalQuantity.NUMBER, ReportingUnit.NUMBER,
                        User.SUPER_USER);
            }
        });

        // inventory item type cannot be invalid
        ensureResourceNotFoundException(new ICode() {
            @Override
            public void run() {
                InventoryItem.create(client, sgp.groupId, 100, "Himalayas",
                        PhysicalQuantity.NUMBER, ReportingUnit.NUMBER,
                        User.SUPER_USER);
            }
        });

        // same name cannot repeat within a group
        ensureAPIException(Status.ERROR_RESOURCE_ALREADY_EXISTS, new ICode() {

            @Override
            public void run() {
                InventoryItem.create(client, sgp.groupId,
                        book.inventoryItemTypeId, "Forest Flower",
                        PhysicalQuantity.NUMBER, ReportingUnit.NUMBER,
                        User.SUPER_USER);
            }
        });

        // but same name can repeat within another group
        forestFlower = InventoryItem.create(client, kl.groupId,
                book.inventoryItemTypeId, "Forest Flower",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);
        assertEquals(kl.groupId, forestFlower.groupId);

        // need permission
        ensureAPIException(Status.ERROR_INSUFFICIENT_PERMISSION, new ICode() {

            @Override
            public void run() {
                InventoryItem.create(client, kl.groupId,
                        rudraksh.inventoryItemTypeId, "Gowri Shankar",
                        PhysicalQuantity.NUMBER, ReportingUnit.NUMBER,
                        validUser);
            }
        });

        // user with permission for sgp cannot create for kl
        ensureAPIException(Status.ERROR_INSUFFICIENT_PERMISSION, new ICode() {

            @Override
            public void run() {
                InventoryItem.create(client, kl.groupId,
                        rudraksh.inventoryItemTypeId, "Gowri Shankar",
                        PhysicalQuantity.NUMBER, ReportingUnit.NUMBER,
                        userWithPermission);
            }
        });

        // user with permssion can create in singapore
        InventoryItem.create(client, sgp.groupId, rudraksh.inventoryItemTypeId,
                "Gowri Shankar", PhysicalQuantity.NUMBER, ReportingUnit.NUMBER,
                userWithPermission);
        // no exception
    }

    @Test
    public void updateTest() {
        InventoryItemProp forestFlower = InventoryItem.create(client,
                sgp.groupId, rudraksh.inventoryItemTypeId, "Flower Forest",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);

        // create another book - mystic eye
        InventoryItem.create(client, sgp.groupId, rudraksh.inventoryItemTypeId,
                "Mystic Eye", PhysicalQuantity.NUMBER, ReportingUnit.NUMBER,
                User.SUPER_USER);

        assertEquals(1, forestFlower.inventoryItemId);

        // change name to Flower-Forest
        forestFlower = InventoryItem.update(client,
                forestFlower.inventoryItemId, book.inventoryItemTypeId,
                "Flower-Forest", null, userWithPermission);

        assertEquals(forestFlower.inventoryItemTypeId, book.inventoryItemTypeId);
        assertEquals("Flower-Forest", forestFlower.displayName);

        // change name to forest-flower
        forestFlower = InventoryItem.update(client,
                forestFlower.inventoryItemId, book.inventoryItemTypeId,
                "Forest-Flower", null, userWithPermission);

        assertEquals(forestFlower.inventoryItemTypeId, book.inventoryItemTypeId);
        assertEquals("Forest-Flower", forestFlower.displayName);
        assertEquals("forestflower", forestFlower.name);

        // new reporting unit should be valid
        final long inventoryItemId = forestFlower.inventoryItemId;
        ensureResourceIncorrectException(new ICode() {

            @Override
            public void run() {
                InventoryItem.update(client, inventoryItemId,
                        book.inventoryItemTypeId, null, ReportingUnit.ML,
                        userWithPermission);
            }
        });

        // need permission for updating
        ensureAPIException(Status.ERROR_INSUFFICIENT_PERMISSION, new ICode() {

            @Override
            public void run() {
                InventoryItem.update(client, inventoryItemId,
                        book.inventoryItemTypeId, null, ReportingUnit.ML,
                        validUser);
            }
        });

        // first char of display name should be alphanumeric
        ensureResourceIncorrectException(new ICode() {

            @Override
            public void run() {
                InventoryItem.update(client, inventoryItemId,
                        book.inventoryItemTypeId, "$Forest Flower",
                        ReportingUnit.ML, userWithPermission);
            }
        });

        // cannot change name to already existing mystic eye
        ensureAPIException(Status.ERROR_RESOURCE_ALREADY_EXISTS, new ICode() {

            @Override
            public void run() {
                InventoryItem.update(client, inventoryItemId,
                        book.inventoryItemTypeId, "mystic-Eye",
                        ReportingUnit.ML, userWithPermission);
            }
        });
    }

    @Test
    public void queryTest() {
        InventoryItemProp forestFlower = InventoryItem.create(client,
                sgp.groupId, book.inventoryItemTypeId, "Forest Flower",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);
        assertEquals(1, forestFlower.inventoryItemId);

        InventoryItemProp mysticEye = InventoryItem.create(client, sgp.groupId,
                book.inventoryItemTypeId, "Mystic Eye",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);
        assertEquals(2, mysticEye.inventoryItemId);

        InventoryItemProp midnights = InventoryItem.create(client, kl.groupId,
                book.inventoryItemTypeId, "Midnights with Mystic",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);
        assertEquals(3, midnights.inventoryItemId);

        InventoryItemProp panchamuki = InventoryItem.create(client,
                sgp.groupId, rudraksh.inventoryItemTypeId, "Panchamuki",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);
        assertEquals(4, panchamuki.inventoryItemId);

        InventoryItemQueryCondition qc = new InventoryItemQueryCondition();
        qc.groupId = sgp.groupId;
        List<InventoryItemProp> props = InventoryItem.query(client, qc,
                User.SUPER_USER);

        assertEquals(3, props.size());
        // should be sorted by inventory type first and then by item name
        assertEquals(forestFlower.inventoryItemId, props.get(0).inventoryItemId);
        assertEquals(mysticEye.inventoryItemId, props.get(1).inventoryItemId);
        assertEquals(panchamuki.inventoryItemId, props.get(2).inventoryItemId);

        // query for all books
        qc = new InventoryItemQueryCondition();
        qc.inventoryItemTypeIds = Utils.getSet(book.inventoryItemTypeId);

        props = InventoryItem.query(client, qc, validUser);

        assertEquals(3, props.size());
        // should be sorted by name
        assertEquals(forestFlower.inventoryItemId, props.get(0).inventoryItemId);
        assertEquals(midnights.inventoryItemId, props.get(1).inventoryItemId);
        assertEquals(mysticEye.inventoryItemId, props.get(2).inventoryItemId);

        // query for books and rudraksh
        qc = new InventoryItemQueryCondition();
        qc.inventoryItemTypeIds.add(book.inventoryItemTypeId);
        qc.inventoryItemTypeIds.add(rudraksh.inventoryItemTypeId);
        props = InventoryItem.query(client, qc, validUser);
        assertEquals(4, props.size());

        // should be sorted by inventory type first and then by item name
        assertEquals(forestFlower.inventoryItemId, props.get(0).inventoryItemId);
        assertEquals(midnights.inventoryItemId, props.get(1).inventoryItemId);
        assertEquals(mysticEye.inventoryItemId, props.get(2).inventoryItemId);
        assertEquals(panchamuki.inventoryItemId, props.get(3).inventoryItemId);

        // query by first char
        qc = new InventoryItemQueryCondition();
        qc.firstChars = Utils.getList("m");
        qc.groupId = sgp.groupId;
        props = InventoryItem.query(client, qc, validUser);
        assertEquals(1, props.size());

        // query for everything in KL
        qc = new InventoryItemQueryCondition();
        qc.groupId = kl.groupId;
        props = InventoryItem.query(client, qc, validUser);
        assertEquals(1, props.size());
        assertEquals(midnights.inventoryItemId, props.get(0).inventoryItemId);

        // check in 5 midnights book at 200 MYR per book
        InventoryItem.checkIn(client, midnights.inventoryItemId, null, 5,
                ReportingUnit.NUMBER, 200, Currency.MYR, "5 @ 200 MYR / Book",
                User.SUPER_USER);
        props = InventoryItem.query(client, qc, validUser);
        assertEquals(1, props.size());
        assertEquals(midnights.inventoryItemId, props.get(0).inventoryItemId);
        assertTrue(5.0 == props.get(0).availableQtyInReportingUnit);
        assertTrue(200 == props.get(0).avgPricePerReportingUnit);
        assertEquals(Currency.MYR, props.get(0).ccy);

        // check in 5 more at 300 MYR per book
        InventoryItem.checkIn(client, midnights.inventoryItemId, null, 5,
                ReportingUnit.NUMBER, 300, Currency.MYR, "5 @ 300 MYR / Book",
                User.SUPER_USER);
        props = InventoryItem.query(client, qc, validUser);
        assertEquals(1, props.size());
        assertEquals(midnights.inventoryItemId, props.get(0).inventoryItemId);
        assertTrue(10 == props.get(0).availableQtyInReportingUnit);
        assertTrue(250 == props.get(0).avgPricePerReportingUnit);
        assertEquals(Currency.MYR, props.get(0).ccy);

        // check out 3 books
        InventoryItem.checkOut(client, midnights.inventoryItemId, null, 3,
                ReportingUnit.NUMBER, null, null, "check out 3 books", null,
                User.SUPER_USER);
        props = InventoryItem.query(client, qc, validUser);
        assertEquals(1, props.size());
        assertEquals(midnights.inventoryItemId, props.get(0).inventoryItemId);
        assertTrue(7 == props.get(0).availableQtyInReportingUnit);
        assertTrue(Math.abs(271.42 - props.get(0).avgPricePerReportingUnit) < 0.1);
        // (2 * 200 + 5 * 300)/7 = 271.42
        assertEquals(Currency.MYR, props.get(0).ccy);

        // check out 3 more books
        InventoryItem.checkOut(client, midnights.inventoryItemId, null, 3,
                ReportingUnit.NUMBER, null, null, "check out 3 books", null,
                User.SUPER_USER);
        props = InventoryItem.query(client, qc, validUser);
        assertEquals(1, props.size());
        assertEquals(midnights.inventoryItemId, props.get(0).inventoryItemId);
        assertTrue(4 == props.get(0).availableQtyInReportingUnit);
        assertTrue(300 == props.get(0).avgPricePerReportingUnit);
        assertEquals(Currency.MYR, props.get(0).ccy);
    }

    @Test
    public void queryStockChangesTest() {
        final InventoryItemProp forestFlower = InventoryItem.create(client,
                sgp.groupId, book.inventoryItemTypeId, "Forest Flower",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);

        assertEquals(1, forestFlower.inventoryItemId);

        InventoryItem.checkIn(client, forestFlower.inventoryItemId, null, 200,
                ReportingUnit.NUMBER, 1, Currency.SGD, "200 @ 1 SGD / Book",
                userWithPermission);

        StockChangeQueryCondition qc = new StockChangeQueryCondition(
                sgp.groupId, new Date().getTime() - 200,
                new Date().getTime() + 100);
        qc.includeCheckIn = true;
        List<StockChangeProp> props = InventoryItem.queryStockChanges(client,
                qc, userWithPermission);
        assertEquals(1, props.size());
        assertEquals(Currency.SGD, props.get(0).ccy);
        assertTrue(200.0 == props.get(0).changeInReportingUnit);
        assertEquals(CheckInOrOut.CHECK_IN, props.get(0).checkInOrOut);
        assertEquals(userWithPermission, props.get(0).login);
        assertEquals(forestFlower.inventoryItemId, props.get(0).inventoryItemId);
        assertEquals("Forest Flower", props.get(0).inventoryItem);
        assertEquals(1, props.get(0).checkInOrOutId);

        // query based on login
        qc.logins.add(validUser);
        props = InventoryItem.queryStockChanges(client, qc, userWithPermission);
        assertEquals(0, props.size());

        qc.logins.add(userWithPermission);
        props = InventoryItem.queryStockChanges(client, qc, userWithPermission);
        assertEquals(1, props.size());

        // include only checkouts
        qc.includeCheckIn = false;
        qc.includeCheckOut = true;
        props = InventoryItem.queryStockChanges(client, qc, userWithPermission);
        assertEquals(0, props.size());

        InventoryItem.checkOut(client, forestFlower.inventoryItemId, null, 100,
                ReportingUnit.NUMBER, 1.2, Currency.SGD, null,
                Utils.getSet("tag1"), userWithPermission);
        props = InventoryItem.queryStockChanges(client, qc, userWithPermission);
        assertEquals(1, props.size());
        assertEquals(1, props.get(0).tags.size());
        assertTrue(props.get(0).tags.contains("tag1"));
    }

    @Test
    public void tagsSavedDuringCheckout() {
        final InventoryItemProp forestFlower = InventoryItem.create(client,
                sgp.groupId, book.inventoryItemTypeId, "Forest Flower",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);

        assertEquals(1, forestFlower.inventoryItemId);

        InventoryItem.checkIn(client, forestFlower.inventoryItemId, null, 200,
                ReportingUnit.NUMBER, 1, Currency.SGD, "200 @ 1 SGD / Book",
                userWithPermission);

        InventoryCheckOutProp inventoryCheckOutProp = InventoryItem.checkOut(
                client, forestFlower.inventoryItemId, null, 100,
                ReportingUnit.NUMBER, 1.2, Currency.SGD, null,
                Utils.getSet("tag1"), userWithPermission);
        assertEquals(1, inventoryCheckOutProp.tags.size());
        assertTrue(inventoryCheckOutProp.tags.contains("tag1"));
    }

    @Test
    public void checkInTest() {
        final InventoryItemProp forestFlower = InventoryItem.create(client,
                sgp.groupId, book.inventoryItemTypeId, "Forest Flower",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);

        assertEquals(1, forestFlower.inventoryItemId);

        final Date date = new Date();

        // permission required for check in
        ensureAPIException(Status.ERROR_INSUFFICIENT_PERMISSION, new ICode() {

            @Override
            public void run() {
                InventoryItem.checkIn(client, forestFlower.inventoryItemId,
                        date, 200, ReportingUnit.NUMBER, 1, Currency.SGD,
                        "200 @ 1 SGD / Book", validUser);
            }
        });

        long ms = new Date().getTime();

        InventoryCheckInProp prop = InventoryItem.checkIn(client,
                forestFlower.inventoryItemId, date, 200, ReportingUnit.NUMBER,
                1, Currency.SGD, "200 @ 1 SGD / Book", userWithPermission);
        assertEquals(1, prop.checkInId);
        assertTrue(Math.abs(prop.timestamp.getTime() - ms) < 10);
        assertEquals(prop.timestamp.getTime(), date.getTime());
        assertEquals(true, prop.available);
        assertTrue(200.0 == prop.availableQtyInDefaultUnit);
        assertEquals(Currency.SGD, prop.ccy);
        assertEquals(ReportingUnit.NUMBER, prop.defaultUnit);
        assertTrue(1.0 == prop.pricePerDefaultUnit);
        assertTrue(200.0 == prop.qtyInDefaultUnit);
        assertEquals(prop.login, userWithPermission);

        InventoryItemProp inventoryItemProp = InventoryItem.safeGet(client,
                forestFlower.inventoryItemId).toProp();
        InventoryItemProp.populateDependents(client,
                Utils.getList(inventoryItemProp));
        assertTrue(200 == inventoryItemProp.availableQtyInReportingUnit);
        assertEquals(Currency.SGD, inventoryItemProp.ccy);
        assertTrue(1 == inventoryItemProp.avgPricePerReportingUnit);

        InventoryItem.checkIn(client, forestFlower.inventoryItemId, date, 200,
                ReportingUnit.NUMBER, 2, Currency.SGD, "200 @ 1 SGD / Book",
                userWithPermission);

        inventoryItemProp = InventoryItem.safeGet(client,
                forestFlower.inventoryItemId).toProp();
        InventoryItemProp.populateDependents(client,
                Utils.getList(inventoryItemProp));
        assertTrue(400 == inventoryItemProp.availableQtyInReportingUnit);
        assertEquals(Currency.SGD, inventoryItemProp.ccy);
        assertTrue(1.5 == inventoryItemProp.avgPricePerReportingUnit);
    }

    @Test
    public void checkOutTest() {
        final InventoryItemProp forestFlower = InventoryItem.create(client,
                sgp.groupId, book.inventoryItemTypeId, "Forest Flower",
                PhysicalQuantity.NUMBER, ReportingUnit.NUMBER, User.SUPER_USER);

        assertEquals(1, forestFlower.inventoryItemId);

        Date date = new Date();
        // check in 200 forest flowers @ 1 SGD per book
        InventoryItem.checkIn(client, forestFlower.inventoryItemId, date, 200,
                ReportingUnit.NUMBER, 1, Currency.SGD, "200 @ 1 SGD / Book",
                userWithPermission);

        // check in 200 more @ 2 SGD per book
        InventoryItem.checkIn(client, forestFlower.inventoryItemId, date, 200,
                ReportingUnit.NUMBER, 2, Currency.SGD, "200 @ 1 SGD / Book",
                userWithPermission);

        // check out 300 without specifying price
        InventoryCheckOutProp checkOutProp = InventoryItem.checkOut(client,
                forestFlower.inventoryItemId, date, 300, ReportingUnit.NUMBER,
                null, Currency.SGD, "checking out 300", null,
                userWithPermission);

        assertTrue(Math.abs(1.3333 - checkOutProp.avgPricePerDefaultUnit) < 0.01);
        // (200 * 1 + 100 * 2)/(100 + 200) = 133.33
    }
}
