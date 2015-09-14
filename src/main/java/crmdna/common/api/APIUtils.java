package crmdna.common.api;

import crmdna.common.Utils;
import crmdna.common.api.APIResponse.Status;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.AssertUtils.ensureNotNull;

public class APIUtils {
    public static APIResponse toAPIResponse(Exception ex, Boolean showStackTrace,
                                            RequestInfo requestInfo) {

        APIResponse apiResponse;
        if (ex instanceof APIException) {
            APIException apiException = (APIException) ex;
            apiResponse = apiException.toAPIResponse();

            if (apiResponse.statusCode == Status.ERROR_INTERNAL)
                Utils.sendAlertEmailToDevTeam(apiException, requestInfo);

        } else {
            apiResponse = new APIResponse();
            apiResponse.statusCode = Status.ERROR_UNHANDLED_EXCEPTION;
            apiResponse.userFriendlyMessage = ex.getMessage();
            // apiResponse.stackTraceElements = ex.getStackTrace();
            apiResponse.stackTrace = getStackTrace(ex);

            Utils.sendAlertEmailToDevTeam(ex, requestInfo);
        }

        if ((showStackTrace == null) || (showStackTrace == false)) {
            // apiResponse.stackTraceElements = null;
            apiResponse.stackTrace = null;
        }

        return apiResponse;
    }

    static List<StackTraceElementProp> getStackTrace(Throwable throwable) {
        ensureNotNull(throwable, "throwable is null");
        ensureNotNull(throwable.getStackTrace(), "stack trace is null");

        List<StackTraceElementProp> stackTrace = new ArrayList<>(throwable.getStackTrace().length);

        for (int i = 0; i < throwable.getStackTrace().length; i++) {
            StackTraceElement element = throwable.getStackTrace()[i];

            StackTraceElementProp prop = new StackTraceElementProp();
            prop.className = element.getClassName();
            prop.fileName = element.getFileName();
            prop.isNativeMethod = element.isNativeMethod();
            prop.lineNo = element.getLineNumber();
            prop.methodName = element.getMethodName();

            stackTrace.add(prop);
        }

        return stackTrace;
    }
}
