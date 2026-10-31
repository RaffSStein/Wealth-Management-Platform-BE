package raff.stein.platformcore.exception;

import lombok.Getter;
import org.openapitools.model.ErrorCategory;

@Getter
public enum ErrorCode {
    UNSPECIFIED_ERROR("Unspecified error", ErrorCategory.GENERIC_ERROR),
    REQUEST_VALIDATION_ERROR("For field [%s] value [%s] is not valid", ErrorCategory.VALIDATION_ERROR),
    OBJECT_NOT_FOUND("Object with id [%s] not found", ErrorCategory.NOT_FOUND_ERROR),
    ACCESS_DENIED("Access denied for user [%s] to object with id [%s]", ErrorCategory.AUTHORIZATION_ERROR),
    AUTHENTICATION_ERROR("Authentication failed for user [%s]", ErrorCategory.AUTHENTICATION_ERROR),
    MISSING_HEADER_ERROR("Missing required header [%s]", ErrorCategory.VALIDATION_ERROR),
    MISSING_HEADER_TOKEN_FIELD("Missing required token field [%s]", ErrorCategory.AUTHENTICATION_ERROR),
    NULL_WMP_CONTEXT("Context was null in an authorized scenario", ErrorCategory.AUTHENTICATION_ERROR),
    OPTIMISTIC_LOCKING_ERROR("The resource has been modified by another transaction. Please retry", ErrorCategory.VALIDATION_ERROR),
    NOT_IMPLEMENTED_ERROR("This feature [%s] is not implemented yet", ErrorCategory.GENERIC_ERROR),
    INVALID_STATE_TRANSITION("Cannot [%s] when current state is [%s]. Allowed states: [%s]", ErrorCategory.VALIDATION_ERROR),
    TOKEN_STORE_UNAVAILABLE("Token store unavailable during [%s]", ErrorCategory.SERVER_ERROR),
    REAUTHENTICATION_REQUIRED("This operation requires a login within the last [%s] seconds", ErrorCategory.AUTHENTICATION_ERROR),
    DEPENDENCY_UNAVAILABLE("Service [%s] is unavailable", ErrorCategory.SERVER_ERROR),
    ROLE_ASSIGNMENT_NOT_ALLOWED("Not allowed: [%s]", ErrorCategory.AUTHORIZATION_ERROR),
    ROLE_ASSIGNMENT_ALREADY_EXISTS("User [%s] already has or awaits role [%s] in [%s/%s]", ErrorCategory.VALIDATION_ERROR),
    ROLE_ASSIGNMENT_REQUEST_NOT_PENDING("Role assignment request [%s] is [%s], not PENDING", ErrorCategory.VALIDATION_ERROR),
    PASSWORD_SETUP_TOKEN_INVALID("Password setup token is expired, already used or invalid", ErrorCategory.AUTHENTICATION_ERROR),

    // TODO: move to specific modules
    // Domain specific errors
    // Document related errors
    DOCUMENT_UPLOAD_VALIDATION_ERROR("File validation not passed. Field [%s] - Value [%s]", ErrorCategory.VALIDATION_ERROR),
    FILE_VALIDATION_FAILED("File validation failed for customer with id [%s].", ErrorCategory.VALIDATION_ERROR),
    DOCUMENT_STORAGE_ERROR("Error while storing document [%s] in storage", ErrorCategory.GENERIC_ERROR);


    private final String messageTemplate;
    private final ErrorCategory errorCategory;

    ErrorCode(String messageTemplate, ErrorCategory errorCategory) {
        this.messageTemplate = messageTemplate;
        this.errorCategory = errorCategory;
    }
}
