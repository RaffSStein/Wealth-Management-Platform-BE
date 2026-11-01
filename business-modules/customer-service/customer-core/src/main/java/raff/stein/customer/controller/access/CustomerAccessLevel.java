package raff.stein.customer.controller.access;

public enum CustomerAccessLevel {
    /**
     * Read the customer in the {@code customerId} path variable.
     */
    READ,
    /**
     * Change the customer in the {@code customerId} path variable.
     */
    WRITE,
    /**
     * List the customers of the user in the {@code userId} path variable.
     */
    LIST_BY_USER
}
