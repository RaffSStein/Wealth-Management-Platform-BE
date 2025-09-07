package raff.stein.customer.service.update.visitor;

import lombok.NonNull;
import raff.stein.customer.model.bo.customer.Customer;

public interface CustomerVisitor <T> {

    Customer visit(Customer customer, @NonNull T payload);

    TypeKey getPayloadType();
}
