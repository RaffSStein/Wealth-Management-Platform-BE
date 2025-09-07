package raff.stein.platformcore.exception.types.unauthorized;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

import java.util.function.Supplier;

public class JwtTokenException extends GenericException {

    public JwtTokenException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static void of(ErrorCode errorCode, String missingToken) {
        String template = errorCode.getMessageTemplate();
        String errorMessage = String.format(template, missingToken);
        throw new JwtTokenException(errorCode, errorMessage);
    }

    public static Supplier<JwtTokenException> with(String missingToken) {
        String message = String.format(ErrorCode.MISSING_HEADER_ERROR.getMessageTemplate(), missingToken);
        return () -> new JwtTokenException(ErrorCode.MISSING_HEADER_ERROR, message);
    }
}
