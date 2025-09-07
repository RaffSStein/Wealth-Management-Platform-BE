package raff.stein.platformcore.exception.types.forbidden;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

public class AccessDeniedException extends GenericException {

    public AccessDeniedException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static void of(ErrorCode errorCode, String userEmail) {
        String template = errorCode.getMessageTemplate();
        String errorMessage = String.format(template, userEmail);
        throw new AccessDeniedException(errorCode, errorMessage);
    }
}
