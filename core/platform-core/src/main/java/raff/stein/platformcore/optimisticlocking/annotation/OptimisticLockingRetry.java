package raff.stein.platformcore.optimisticlocking.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare optimistic locking retry semantics on a method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
        retryFor = {
                ObjectOptimisticLockingFailureException.class,
                OptimisticLockingFailureException.class
        },
        maxAttemptsExpression = "#{@optimisticLockingRetryProperties.maxAttempts}",
        backoff = @Backoff(
                delayExpression = "#{@optimisticLockingRetryProperties.backoffDelayMs}",
                multiplierExpression = "#{@optimisticLockingRetryProperties.backoffMultiplier}"
        )
)
public @interface OptimisticLockingRetry {

    @AliasFor(annotation = Retryable.class, attribute = "maxAttemptsExpression")
    String maxAttemptsExpression() default "";

    @AliasFor(annotation = Retryable.class, attribute = "backoff")
    Backoff backoff() default @Backoff;
}
