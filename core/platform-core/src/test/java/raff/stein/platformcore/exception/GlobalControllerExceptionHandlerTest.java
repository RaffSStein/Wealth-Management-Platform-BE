package raff.stein.platformcore.exception;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.openapitools.model.ErrorCategory;
import org.openapitools.model.ErrorResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.databind.exc.InvalidFormatException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GlobalControllerExceptionHandlerTest {

    private final GlobalControllerExceptionHandler handler = new GlobalControllerExceptionHandler(mock(Tracer.class));

    @Test
    void shouldReturnFieldAndValueForInvalidEnumValue() {
        final InvalidFormatException cause = InvalidFormatException.from(null, "Unexpected value", "NOT_A_GENDER", String.class);
        cause.prependPath(Object.class, "gender");
        final HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("JSON parse error", cause, mock(HttpInputMessage.class));

        final ErrorResponse response = handler.handleUnreadableRequestBody(ex);

        assertEquals("For field [gender] value [NOT_A_GENDER] is not valid", response.getErrorMessage());
        assertEquals(ErrorCode.REQUEST_VALIDATION_ERROR.name(), response.getErrorCode());
        assertEquals(ErrorCategory.VALIDATION_ERROR, response.getCategory());
    }

    @Test
    void shouldReturnGenericMessageForMalformedJson() {
        final HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new IllegalStateException("Unexpected end-of-input"), mock(HttpInputMessage.class));

        final ErrorResponse response = handler.handleUnreadableRequestBody(ex);

        assertEquals("Malformed request body", response.getErrorMessage());
        assertEquals(ErrorCode.REQUEST_VALIDATION_ERROR.name(), response.getErrorCode());
    }
}
