package raff.stein.platformcore.optimisticlocking.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import raff.stein.platformcore.exception.types.conflict.VersionLockingException;

import java.util.function.Supplier;

/**
 * Utility to execute an operation with retries on optimistic locking failures.
 */
public class OptimisticLockingRetryExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimisticLockingRetryExecutor.class);

    private final int maxAttempts;

    public OptimisticLockingRetryExecutor(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public <T> T executeWithRetry(Supplier<T> supplier) {
        int attempt = 0;
        while (true) {
            try {
                return supplier.get();
            } catch (OptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= maxAttempts) {
                    LOGGER.warn("Optimistic locking retries exhausted after {} attempts", attempt, ex);
                    VersionLockingException.ofOptimisticLocking();
                }
                LOGGER.debug("Optimistic locking failure on attempt {}/{} - retrying", attempt, maxAttempts, ex);
            }
        }
    }
}
