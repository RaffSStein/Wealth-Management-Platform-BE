package raff.stein.platformcore.exception.types.unavailable;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

/**
 * The shared token store can't be reached. Callers fail closed: a token is never accepted (or consumed twice)
 * when its state can't be checked.
 */
public class TokenStoreUnavailableException extends GenericException {

    public TokenStoreUnavailableException(String operation, Throwable cause) {
        super(ErrorCode.TOKEN_STORE_UNAVAILABLE,
                String.format(ErrorCode.TOKEN_STORE_UNAVAILABLE.getMessageTemplate(), operation));
        initCause(cause);
    }
}
