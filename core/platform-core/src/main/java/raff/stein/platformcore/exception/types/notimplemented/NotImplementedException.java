package raff.stein.platformcore.exception.types.notimplemented;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

public class NotImplementedException extends GenericException {

    public NotImplementedException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static void of(ErrorCode errorCode, String featureName) {
        String template = errorCode.getMessageTemplate();
        String errorMessage = String.format(template, featureName);
        throw new NotImplementedException(errorCode, errorMessage);
    }
}
