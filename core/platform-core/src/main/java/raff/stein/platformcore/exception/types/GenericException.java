package raff.stein.platformcore.exception.types;

import lombok.Getter;
import raff.stein.platformcore.exception.ErrorCode;

@Getter
public abstract class GenericException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorMessage;

    protected GenericException(ErrorCode errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    protected GenericException(String errorMessage) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.errorCode = null;
    }

    protected GenericException(ErrorCode errorCode) {
        super(errorCode != null ? errorCode.getMessageTemplate() : "An error occurred");
        this.errorCode = errorCode;
        this.errorMessage = errorCode != null ? errorCode.getMessageTemplate() : "An error occurred";
    }

    public String getErrorCodeAsString() {
        if(errorCode == null) {
            return "UNKNOWN_ERROR";
        }
        return String.valueOf(errorCode);
    }
}
