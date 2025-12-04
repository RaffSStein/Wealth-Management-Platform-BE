package raff.stein.platformcore.optimisticlocking;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import raff.stein.platformcore.optimisticlocking.executor.OptimisticLockingRetryExecutor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptimisticLockingRetryExecutorTest {

    @Test
    void shouldRetryAndSucceedWithinMaxAttempts() {
        OptimisticLockingRetryExecutor executor = new OptimisticLockingRetryExecutor(3);
        AtomicInteger counter = new AtomicInteger();

        String result = executor.executeWithRetry(() -> {
            if (counter.getAndIncrement() < 2) {
                throw new OptimisticLockingFailureException("conflict");
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(3, counter.get());
    }

    @Test
    void shouldThrowVersionLockingExceptionAfterMaxAttempts() {
        OptimisticLockingRetryExecutor executor = new OptimisticLockingRetryExecutor(2);
        AtomicInteger counter = new AtomicInteger();

        assertThrows(RuntimeException.class, () -> executor.executeWithRetry(() -> {
            counter.incrementAndGet();
            throw new OptimisticLockingFailureException("conflict");
        }));

        assertEquals(2, counter.get());
    }
}
