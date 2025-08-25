package raff.stein.customer.exception;

import raff.stein.platformcore.exception.types.GenericException;

public class CustomerException extends GenericException {

    public CustomerException(String errorMessage) {
        super(errorMessage);
    }
}
