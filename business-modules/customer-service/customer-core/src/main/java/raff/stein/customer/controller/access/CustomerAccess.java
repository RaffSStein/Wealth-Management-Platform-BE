package raff.stein.customer.controller.access;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares what a controller method does to a customer. {@link CustomerAccessInterceptor} enforces it before the method
 * runs, through {@link raff.stein.customer.service.access.CustomerAccessPolicy}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomerAccess {

    CustomerAccessLevel value();
}
