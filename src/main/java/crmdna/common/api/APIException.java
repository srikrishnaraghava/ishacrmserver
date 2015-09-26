package crmdna.common.api;

import crmdna.common.Utils;
import crmdna.common.api.APIResponse.Status;

@SuppressWarnings("serial")
public class APIException extends RuntimeException {
    public Status statusCode;
    public String userFriendlyMessage;
    public Object object;

    public APIException() {
    }

    public APIException(String message) {
        super(message);
        userFriendlyMessage = message;
    }

    public APIException status(Status status) {
        this.statusCode = status;
        return this;
    }

    public APIException message(String message) {
        this.userFriendlyMessage = message;
        return this;
    }

    public APIException object(Object object) {
        this.object = object;
        return this;
    }

    public APIResponse toAPIResponse() {
        APIResponse resp = new APIResponse();

        resp.statusCode = statusCode;
        resp.userFriendlyMessage = userFriendlyMessage;
        resp.object = object;

        resp.stackTrace = Utils.getStackTrace(this);

        return resp;
    }
}
