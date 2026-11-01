package raff.stein.platformcore.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.openapitools.model.ErrorCategory;
import org.openapitools.model.ErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalControllerExceptionHandlerTest {

    private final GlobalControllerExceptionHandler handler = new GlobalControllerExceptionHandler(mock(Tracer.class));
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // mirrors the openapi-generator enum: fromValue throws, so Jackson raises ValueInstantiationException
    enum GeneratedStyleGender {
        MALE, FEMALE;

        @JsonCreator
        public static GeneratedStyleGender fromValue(String value) {
            for (GeneratedStyleGender gender : values()) {
                if (gender.name().equals(value)) {
                    return gender;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    enum PlainGender { MALE, FEMALE }

    record GeneratedStyleRequest(GeneratedStyleGender gender) { }

    record PlainRequest(PlainGender gender) { }

    @Test
    void shouldReturnFieldForValueRejectedByGeneratedEnumCreator() {
        final ErrorResponse response = handle("{\"gender\":\"NOT_SPECIFIED\"}", GeneratedStyleRequest.class);

        assertEquals("Invalid value for field [gender]", response.getErrorMessage());
        assertEquals(ErrorCode.REQUEST_VALIDATION_ERROR.name(), response.getErrorCode());
        assertEquals(ErrorCategory.VALIDATION_ERROR, response.getCategory());
    }

    @Test
    void shouldReturnFieldAndValueForUnknownPlainEnumConstant() {
        final ErrorResponse response = handle("{\"gender\":\"NOT_SPECIFIED\"}", PlainRequest.class);

        assertEquals("For field [gender] value [NOT_SPECIFIED] is not valid", response.getErrorMessage());
    }

    @Test
    void shouldAnswerForbiddenForARoleDeniedByMethodSecurity() throws NoSuchMethodException {
        final ErrorResponse response = handler.handleSpringSecurityAccessDenied(
                new AuthorizationDeniedException("Access Denied"));

        assertEquals("ACCESS_DENIED", response.getErrorCode());
        assertEquals(ErrorCategory.AUTHORIZATION_ERROR, response.getCategory());
        assertEquals(HttpStatus.FORBIDDEN, GlobalControllerExceptionHandler.class
                .getMethod("handleSpringSecurityAccessDenied", AccessDeniedException.class)
                .getAnnotation(ResponseStatus.class).value());
    }

    @Test
    void shouldAnswerBadRequestForArgumentOfTheWrongType() throws NoSuchMethodException {
        final ErrorResponse response = handler.handleArgumentTypeMismatch(new MethodArgumentTypeMismatchException(
                "not-a-uuid",
                UUID.class,
                "customerId",
                mock(MethodParameter.class),
                new IllegalArgumentException("Invalid UUID string")));

        assertEquals("For field [customerId] value [not-a-uuid] is not valid", response.getErrorMessage());
        assertEquals(ErrorCode.REQUEST_VALIDATION_ERROR.name(), response.getErrorCode());
        assertEquals(ErrorCategory.VALIDATION_ERROR, response.getCategory());
        assertEquals(HttpStatus.BAD_REQUEST, GlobalControllerExceptionHandler.class
                .getMethod("handleArgumentTypeMismatch", MethodArgumentTypeMismatchException.class)
                .getAnnotation(ResponseStatus.class).value());
    }

    @Test
    void shouldAnswerBadRequestForAnUnknownSortProperty() throws Exception {
        final PropertyReferenceException exception = mock(PropertyReferenceException.class);
        when(exception.getPropertyName()).thenReturn("nope");

        final ErrorResponse response = handler.handleUnknownSortProperty(exception);

        assertEquals(ErrorCode.REQUEST_VALIDATION_ERROR.name(), response.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, GlobalControllerExceptionHandler.class
                .getMethod("handleUnknownSortProperty", PropertyReferenceException.class)
                .getAnnotation(ResponseStatus.class)
                .value());
    }

    @Test
    void shouldAnswerBadRequestForAConstraintOnAControllerParameter() {
        final ParameterValidationResult result = mock(ParameterValidationResult.class);
        when(result.getResolvableErrors()).thenReturn(List.of(
                new FieldError("customerDto", "taxId", "must not be null"),
                new DefaultMessageSourceResolvable(null, "must not be blank")));
        final HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        when(exception.getParameterValidationResults()).thenReturn(List.of(result));

        final ErrorResponse response = handler.handleMethodValidation(exception);

        assertEquals(ErrorCode.REQUEST_VALIDATION_ERROR.name(), response.getErrorCode());
        assertEquals("taxId: must not be null,must not be blank", response.getErrorMessage());
    }

    @Test
    void shouldReturnGenericMessageForMalformedJson() {
        final ErrorResponse response = handle("{\"gender\":", PlainRequest.class);

        assertEquals("Malformed request body", response.getErrorMessage());
        assertEquals(ErrorCode.REQUEST_VALIDATION_ERROR.name(), response.getErrorCode());
    }

    private ErrorResponse handle(String json, Class<?> type) {
        final JacksonException cause = assertThrows(JacksonException.class, () -> jsonMapper.readValue(json, type));
        final HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("JSON parse error", cause, mock(HttpInputMessage.class));
        return handler.handleUnreadableRequestBody(ex);
    }
}
