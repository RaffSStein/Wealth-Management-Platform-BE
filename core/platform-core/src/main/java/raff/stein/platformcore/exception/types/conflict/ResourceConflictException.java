package raff.stein.platformcore.exception.types.conflict;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

/**
 * The request conflicts with the current state of a resource (e.g. it already exists, or is no longer pending).
 */
public class ResourceConflictException extends GenericException {

    public ResourceConflictException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static ResourceConflictException of(ErrorCode errorCode, Object... values) {
        return new ResourceConflictException(errorCode, String.format(errorCode.getMessageTemplate(), values));
    }
}
