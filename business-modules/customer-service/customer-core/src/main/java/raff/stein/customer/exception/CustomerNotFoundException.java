package raff.stein.customer.exception;

import raff.stein.platformcore.exception.ErrorCode;
import raff.stein.platformcore.exception.types.notfound.GenericObjectNotFoundException;

import java.util.UUID;
import java.util.function.Supplier;

// extends GenericObjectNotFoundException so GlobalControllerExceptionHandler maps it to 404
public class CustomerNotFoundException extends GenericObjectNotFoundException {

    public static final String CUSTOMER_NOT_FOUND_TEMPLATE_MESSAGE = "Customer with id [%s] not found";

    public CustomerNotFoundException(ErrorCode errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    public static Supplier<CustomerNotFoundException> with(UUID customerId) {
        String message = String.format(CUSTOMER_NOT_FOUND_TEMPLATE_MESSAGE, customerId);
        return () -> new CustomerNotFoundException(ErrorCode.OBJECT_NOT_FOUND, message);
    }
}
