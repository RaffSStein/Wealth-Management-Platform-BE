package raff.stein.platformcore.exception.types.unauthorized;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

import java.util.function.Supplier;

public class WmpContextException extends GenericException {

    public WmpContextException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public WmpContextException(ErrorCode errorCode) {
        super(errorCode, errorCode.getMessageTemplate());
    }

    public static Supplier<WmpContextException> forMissingField(String missingField) {
        String message = String.format(ErrorCode.MISSING_HEADER_TOKEN_FIELD.getMessageTemplate(), missingField);
        return () -> new WmpContextException(ErrorCode.MISSING_HEADER_TOKEN_FIELD, message);
    }

    public static Supplier<WmpContextException> forNullContext() {
        return () -> new WmpContextException(ErrorCode.NULL_WMP_CONTEXT);
    }


}
