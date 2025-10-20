package raff.stein.platformcore.exception;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.ErrorCategory;
import org.openapitools.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import raff.stein.platformcore.exception.types.GenericException;
import raff.stein.platformcore.exception.types.badrequest.RequestValidationException;
import raff.stein.platformcore.exception.types.conflict.VersionLockingException;
import raff.stein.platformcore.exception.types.forbidden.AccessDeniedException;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;
import raff.stein.platformcore.exception.types.notimplemented.NotImplementedException;
import raff.stein.platformcore.exception.types.unauthorized.AuthenticationException;
import raff.stein.platformcore.exception.types.unauthorized.JwtTokenException;
import raff.stein.platformcore.exception.types.unauthorized.WmpContextException;

import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalControllerExceptionHandler {

    private final Tracer tracer;

    /**
     * ============= Input validation exception handling =============
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInputValidationException(MethodArgumentNotValidException ex) {

        String errorMessages = ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(","));

        // Log with stack trace at WARN level for validation errors
        log.warn("Validation failed traceId={} errors={}", getTraceId(), errorMessages, ex);

        return new ErrorResponse()
                .errorMessage(errorMessages)
                .errorCode(getErrorCode(ex))
                .category(getErrorCategory(ex))
                .traceId(getTraceId());

    }

    /**
     * ============= BAD REQUEST (400) exception handlers =============
     */
    @ExceptionHandler({
            RequestValidationException.class
            // Add any specific request validation exceptions here
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequestExceptions(GenericException ex) {
        log.warn("Bad request [code={}, category={}] traceId={}", getErrorCode(ex), getErrorCategory(ex), getTraceId(), ex);
        return getErrorResponse(ex);
    }

    /**
     * ============= NOT FOUND (404) exception handlers =============
     */
    @ExceptionHandler({
            GenericObjectNotFoundException.class
            // Add any specific not found exceptions here
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundExceptions(GenericException ex) {
        log.warn("Not found [code={}, category={}] traceId={}", getErrorCode(ex), getErrorCategory(ex), getTraceId(), ex);
        return getErrorResponse(ex);
    }


    /**
     * ============= FORBIDDEN (403) exception handlers =============
     */
    @ExceptionHandler({
            AccessDeniedException.class
            // Add any specific forbidden exceptions here
    })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbiddenExceptions(GenericException ex) {
        log.warn("Forbidden [code={}, category={}] traceId={}", getErrorCode(ex), getErrorCategory(ex), getTraceId(), ex);
        return getErrorResponse(ex);
    }

    /**
     * ============= UNAUTHORIZED (401) exception handlers =============
     */
    @ExceptionHandler({
            JwtTokenException.class,
            AuthenticationException.class,
            WmpContextException.class
            // Add any specific unauthorized exceptions here
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorizedExceptions(GenericException ex) {
        log.warn("Unauthorized [code={}, category={}] traceId={}", getErrorCode(ex), getErrorCategory(ex), getTraceId(), ex);
        return getErrorResponse(ex);
    }

    /**
     * ============= CONFLICT (409) exception handlers =============
     */
    @ExceptionHandler({
            VersionLockingException.class
            // Add any specific conflict exceptions here
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflictExceptions(GenericException ex) {
        log.warn("Conflict [code={}, category={}] traceId={}", getErrorCode(ex), getErrorCategory(ex), getTraceId(), ex);
        return getErrorResponse(ex);
    }

    /**
     * ============= NOT IMPLEMENTED (501) exception handlers =============
     */
    @ExceptionHandler({
            NotImplementedException.class
            // Add any specific not implemented exceptions here
    })
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public ErrorResponse handleNotImplementedExceptions(GenericException ex) {
        log.warn("Not implemented [code={}, category={}] traceId={}", getErrorCode(ex), getErrorCategory(ex), getTraceId(), ex);
        return getErrorResponse(ex);
    }

    /**
     * ============= INTERNAL SERVER ERROR (500) exception handlers =============
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnhandledExceptions(Exception ex) {
        log.error("Unhandled exception traceId={}", getTraceId(), ex);
        return getErrorResponse(ex);
    }


    // utility methods to build the ErrorResponse
    private ErrorResponse getErrorResponse(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        if(ex instanceof GenericException genericException) {
            errorResponse.setErrorMessage(genericException.getErrorMessage());
        } else {
            errorResponse.setErrorMessage(ex.getMessage());
        }
        errorResponse.setErrorCode(getErrorCode(ex));
        errorResponse.setCategory(getErrorCategory(ex));
        errorResponse.setTraceId(getTraceId());
        return errorResponse;
    }

    private ErrorCategory getErrorCategory(Exception ex) {
        // if it's a generic exception it means that it will have an error code and category
        if(ex instanceof GenericException genericException) {
            return genericException.getErrorCode().getErrorCategory();
        } else {
            return ErrorCode.UNSPECIFIED_ERROR.getErrorCategory();
        }

    }

    private String getTraceId() {
        return Optional.ofNullable(tracer.currentSpan())
                .map(Span::context)
                .map(TraceContext::traceId)
                .orElse(null);
    }

    private String getErrorCode(Exception ex) {
        // if it's a generic exception it means that it will have an error code and category
        if(ex instanceof GenericException genericException) {
            return genericException.getErrorCodeAsString();
        } else {
            return ErrorCode.UNSPECIFIED_ERROR.name();
        }
    }
}
