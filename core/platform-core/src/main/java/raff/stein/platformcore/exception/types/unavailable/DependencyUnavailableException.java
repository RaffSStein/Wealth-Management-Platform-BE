package raff.stein.platformcore.exception.types.unavailable;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

/**
 * A platform service needed to complete the request can't be reached. Nothing was changed: the client may retry.
 */
public class DependencyUnavailableException extends GenericException {

    public DependencyUnavailableException(String dependency, Throwable cause) {
        super(ErrorCode.DEPENDENCY_UNAVAILABLE,
                String.format(ErrorCode.DEPENDENCY_UNAVAILABLE.getMessageTemplate(), dependency));
        initCause(cause);
    }
}
