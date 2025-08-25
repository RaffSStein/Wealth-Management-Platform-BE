package raff.stein.document.exception;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

import java.util.function.Supplier;

public class StorageException extends GenericException {

    public StorageException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static void of(ErrorCode errorCode, String documentName) {
        String template = errorCode.getMessageTemplate();
        String errorMessage = String.format(template, documentName);
        throw new StorageException(errorCode, errorMessage);
    }

    public static Supplier<StorageException> with(String documentName) {
        String message = String.format(ErrorCode.DOCUMENT_STORAGE_ERROR.getMessageTemplate(), documentName);
        return () -> new StorageException(ErrorCode.DOCUMENT_STORAGE_ERROR, message);
    }

}
