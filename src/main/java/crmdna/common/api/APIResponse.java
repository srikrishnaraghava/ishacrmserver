package crmdna.common.api;

import crmdna.common.StackTraceElementProp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class APIResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    public Status statusCode;
    public String userFriendlyMessage;
    public Object object;
    public List<StackTraceElementProp> stackTrace = new ArrayList<>();
    public Long processingTimeInMS;
    public APIResponse() {
    }

    public APIResponse status(Status status) {
        this.statusCode = status;
        return this;
    }

    public APIResponse message(String userFriendlyMessage) {
        this.userFriendlyMessage = userFriendlyMessage;
        return this;
    }

    public APIResponse object(Object object) {
        this.object = object;
        return this;
    }

    public APIResponse processingTimeInMS(Long ms) {
        this.processingTimeInMS = ms;
        return this;
    }

    public enum Status {
        SUCCESS, ERROR_LOGIN_REQUIRED, ERROR_INVALID_USER, ERROR_INSUFFICIENT_PERMISSION, ERROR_RESOURCE_NOT_FOUND, ERROR_RESOURCE_ALREADY_EXISTS, ERROR_RESOURCE_NOT_FULLY_SPECIFIED, ERROR_RESOURCE_INCORRECT, ERROR_PRECONDITION_FAILED, ERROR_UNHANDLED_EXCEPTION, ERROR_NOT_IMPLEMENTED, ERROR_INTERNAL, ERROR_OPERATION_NOT_ALLOWED, PAYPAL_ERROR, ERROR_OVERFLOW, ERROR_AUTH_FAILURE, ERROR_INVALID_INPUT, ERROR_INVALID_SETUP, ASYNC_CALL_SUBMITTED
    }
}
