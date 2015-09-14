package crmdna.common;

import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

public class AssertUtils {
    public static void ensure(boolean condition) {
        ensure(condition, "Pre condition failure");
    }

    public static void ensure(boolean condition, String messageIfError) {
        if (!condition)
            throw new APIException(messageIfError + " (AssertUtils.ensure failed)")
                    .status(Status.ERROR_RESOURCE_INCORRECT);
    }

    public static void ensureNotNull(Object obj, String messageIfError) {
        if (obj == null)
            throw new APIException(messageIfError + " (AssertUtils.ensureNotNull failed)")
                    .status(Status.ERROR_RESOURCE_INCORRECT);
    }

    public static void ensureNotNullNotEmpty(String s, String messageIfError) {
        if ((s == null) || s.isEmpty())
            throw new APIException(messageIfError + " (AssertUtils.ensureNotNull failed)")
                    .status(Status.ERROR_RESOURCE_INCORRECT);
    }

    public static void ensureNoNullElement(Iterable<? extends Object> objs) {
        if (objs == null)
            throw new APIException("objs itself is null (AssertUtils.ensureNoNullElement failed)")
                    .status(Status.ERROR_RESOURCE_INCORRECT);

        int index = 0;
        for (Object obj : objs) {
            if (obj == null)
                throw new APIException("Null element found at (zero based) index [" + index
                        + "] (AssertUtils.ensureNoNullElement failed)").status(Status.ERROR_RESOURCE_INCORRECT);
        }
    }

    public static void ensureNotNull(Object obj) {
        ensureNotNull(obj, "Found null object");
    }

    public static void ensureEqual(int expected, int actual, String messageIfError) {

        if (expected != actual) {
            String errMessage =
                    "Expected [" + expected + "] actual [" + actual + "] (AssertUtils.assertEquals failed)";

            if (messageIfError != null) {
                errMessage = messageIfError + ". " + errMessage;
            }

            throw new APIException(errMessage).status(Status.ERROR_RESOURCE_INCORRECT);
        }
    }

    public static void ensureEqual(int expected, int actual) {

        ensureEqual(expected, actual, null);
    }
}
