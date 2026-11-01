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
    LIST_BY_USER,
    /**
     * List or count the customers the caller can reach; no customer in the path.
     */
    LIST,
    /**
     * Read the counts of the advisor's dashboard; no customer in the path.
     */
    ADVISOR_DASHBOARD
}
