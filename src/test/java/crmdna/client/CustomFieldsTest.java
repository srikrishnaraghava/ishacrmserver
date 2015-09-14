package crmdna.client;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.CustomFields.CustomFieldProp;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomFieldsTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void createTest() {
        ClientProp client = Client.create("Isha");

        CustomFields.create(client.name, "singapore nric");
        CustomFields.create(client.name, "international passport");
        CustomFields.create(client.name, "indian pan number");

        List<CustomFieldProp> props = CustomFields.getAll(client.name);
        assertEquals(3, props.size());
        assertEquals(0, props.get(0).id);
        assertEquals("singapore nric", props.get(0).name);
        assertEquals(true, props.get(0).enabled);

        assertEquals(1, props.get(1).id);
        assertEquals("international passport", props.get(1).name);
        assertEquals(true, props.get(1).enabled);

        assertEquals(2, props.get(2).id);
        assertEquals("indian pan number", props.get(2).name);
        assertEquals(true, props.get(2).enabled);

        // cannot create duplicate
        try {
            CustomFields.create(client.name, "Singapore NRIC");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // cannot add custom field to a non existent client
        try {
            CustomFields.create("dummy client", "Singapore NRIC");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void getAllTest() {
        ClientProp client = Client.create("Isha");

        // empty list when no custom fields are present
        List<CustomFieldProp> props = CustomFields.getAll(client.name);
        assertEquals(0, props.size());

        CustomFields.create(client.name, "singapore nric");
        CustomFields.create(client.name, "international passport");
        CustomFields.create(client.name, "indian pan number");

        props = CustomFields.getAll(client.name);
        assertEquals(3, props.size());
        assertEquals(0, props.get(0).id);
        assertEquals("singapore nric", props.get(0).name);
        assertEquals(true, props.get(0).enabled);

        assertEquals(1, props.get(1).id);
        assertEquals("international passport", props.get(1).name);
        assertEquals(true, props.get(1).enabled);

        assertEquals(2, props.get(2).id);
        assertEquals("indian pan number", props.get(2).name);
        assertEquals(true, props.get(2).enabled);

        // exception for non existent client
        try {
            CustomFields.getAll("dummy client");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void updateTest() {
        ClientProp client = Client.create("Isha");

        // cannot update when no custom fields are present
        try {
            CustomFields.update(client.name, 3, "newFieldName", true);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        CustomFields.create(client.name, "singapore nric"); // 0
        CustomFields.create(client.name, "international passport"); // 1
        CustomFields.create(client.name, "indian pan number"); // 2

        // cannot update non existent field id
        try {
            CustomFields.update(client.name, 6, "newFieldName", true);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // cannot rename to an another field
        try {
            CustomFields.update(client.name, 0, "International Passport", true);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // can change case
        CustomFields.update(client.name, 0, "Singapore NRIC", true);
        CustomFieldProp prop = CustomFields.getAll(client.name).get(0);
        assertEquals(0, prop.id);
        assertEquals("Singapore NRIC", prop.name);
        assertEquals(true, prop.enabled);

        // can disable a field
        CustomFields.update(client.name, 2, "Indian PAN No", false);
        prop = CustomFields.getAll(client.name).get(2);
        assertEquals(2, prop.id);
        assertEquals("Indian PAN No", prop.name);
        assertEquals(false, prop.enabled);

        // can enable a field
        CustomFields.update(client.name, 2, "Indian PAN No", true);
        prop = CustomFields.getAll(client.name).get(2);
        assertEquals(2, prop.id);
        assertEquals("Indian PAN No", prop.name);
        assertEquals(true, prop.enabled);

        // null should preserve existing value
        CustomFields.update(client.name, 2, "Indian PAN No.", null);
        prop = CustomFields.getAll(client.name).get(2);
        assertEquals(2, prop.id);
        assertEquals("Indian PAN No.", prop.name);
        assertEquals(true, prop.enabled);

        CustomFields.update(client.name, 2, null, false);
        prop = CustomFields.getAll(client.name).get(2);
        assertEquals(2, prop.id);
        assertEquals("Indian PAN No.", prop.name);
        assertEquals(false, prop.enabled);

        CustomFields.update(client.name, 2, null, null);
        prop = CustomFields.getAll(client.name).get(2);
        assertEquals(2, prop.id);
        assertEquals("Indian PAN No.", prop.name);
        assertEquals(false, prop.enabled);
    }
}
