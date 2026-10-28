package raff.stein.platformcore.exception.types.conflict;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

public class InvalidStateTransitionException extends GenericException {

    public InvalidStateTransitionException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static InvalidStateTransitionException of(String action, Object currentState, Object allowedStates) {
        final String errorMessage = String.format(
                ErrorCode.INVALID_STATE_TRANSITION.getMessageTemplate(), action, currentState, allowedStates);
        return new InvalidStateTransitionException(ErrorCode.INVALID_STATE_TRANSITION, errorMessage);
    }
}
