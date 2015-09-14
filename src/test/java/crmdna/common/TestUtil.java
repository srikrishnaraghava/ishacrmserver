package crmdna.common;

import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestUtil {
    public static void ensureAPIException(Status status, ICode code) {
        try {
            code.run();
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(status, ex.statusCode);
        }
    }

    public static void ensureResourceIncorrectException(ICode code) {
        try {
            code.run();
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    public static void ensureResourceNotFoundException(ICode code) {
        try {
            code.run();
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }
}
