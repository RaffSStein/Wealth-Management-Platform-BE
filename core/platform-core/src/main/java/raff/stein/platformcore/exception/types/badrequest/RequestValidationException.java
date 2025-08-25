package raff.stein.platformcore.exception.types.badrequest;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

public class RequestValidationException extends GenericException {

    public RequestValidationException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static void of(ErrorCode errorCode, String fieldName, String fieldValue) {
        String template = errorCode.getMessageTemplate();
        String errorMessage = String.format(template, fieldName, fieldValue);
        throw new RequestValidationException(errorCode, errorMessage);
    }
}
