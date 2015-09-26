package crmdna.common.api;

import crmdna.common.Utils;
import crmdna.common.api.APIResponse.Status;

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
            apiResponse.stackTrace = Utils.getStackTrace(ex);

            Utils.sendAlertEmailToDevTeam(ex, requestInfo);
        }

        if ((showStackTrace == null) || (showStackTrace == false)) {
            // apiResponse.stackTraceElements = null;
            apiResponse.stackTrace = null;
        }

        return apiResponse;
    }
}
