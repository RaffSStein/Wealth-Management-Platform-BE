package raff.stein.platformcore.exception.types.unauthorized;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

public class AuthenticationException extends GenericException {

    public AuthenticationException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static void of(ErrorCode errorCode, String userEmail) {
        String template = errorCode.getMessageTemplate();
        String errorMessage = String.format(template, userEmail);
        throw new AuthenticationException(errorCode, errorMessage);
    }
}
