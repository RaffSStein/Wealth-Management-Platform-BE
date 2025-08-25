package raff.stein.platformcore.exception.types.conflict;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

public class VersionLockingException extends GenericException {

    public VersionLockingException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static void ofOptimisticLocking() {
        throw new VersionLockingException(ErrorCode.OPTIMISTIC_LOCKING_ERROR, ErrorCode.OPTIMISTIC_LOCKING_ERROR.getMessageTemplate());
    }

    public static void of(ErrorCode errorCode, String userEmail) {
        String template = errorCode.getMessageTemplate();
        String errorMessage = String.format(template, userEmail);
        throw new VersionLockingException(errorCode, errorMessage);
    }
}
