package raff.stein.platformcore.exception;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.ErrorCategory;
import org.openapitools.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import raff.stein.platformcore.exception.types.GenericException;
import raff.stein.platformcore.exception.types.unauthorized.AuthenticationException;
import raff.stein.platformcore.exception.types.unauthorized.JwtTokenException;

import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Tracer tracer;


    /**
     * ============= UNAUTHORIZED (401) exception handlers =============
     */
    @ExceptionHandler({
            JwtTokenException.class,
            AuthenticationException.class
            // Add any specific unauthorized exceptions here
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorizedExceptions(GenericException ex) {
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
