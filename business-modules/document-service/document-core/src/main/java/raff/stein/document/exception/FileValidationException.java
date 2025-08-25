package raff.stein.document.exception;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

import java.util.function.Supplier;

public class FileValidationException extends GenericException {

    public FileValidationException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static Supplier<FileValidationException> with(String customerId) {
        String message = String.format(ErrorCode.FILE_VALIDATION_FAILED.getMessageTemplate(), customerId);
        return () -> new FileValidationException(ErrorCode.FILE_VALIDATION_FAILED, message);
    }
}
