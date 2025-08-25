package raff.stein.platformcore.exception.types.notfound;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.GenericException;

import java.util.function.Supplier;

public class GenericObjectNotFoundException extends GenericException {

    public static final String OBJECT_NOT_FOUND_TEMPLATE_MESSAGE = "Object with id [%s] not found";

    public GenericObjectNotFoundException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static void of(ErrorCode errorCode, String fieldName, String fieldValue) {
        String template = errorCode.getMessageTemplate();
        String errorMessage = String.format(template, fieldName, fieldValue);
        throw new GenericObjectNotFoundException(errorCode, errorMessage);
    }

    public static Supplier<GenericObjectNotFoundException> with(String objectId) {
        String message = String.format(OBJECT_NOT_FOUND_TEMPLATE_MESSAGE, objectId);
        return () -> new GenericObjectNotFoundException(ErrorCode.OBJECT_NOT_FOUND, message);
    }
}
