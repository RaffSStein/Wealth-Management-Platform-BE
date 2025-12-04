package raff.stein.platformcore.optimisticlocking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OptimisticLockingRetryTestConfiguration.class, TestOptimisticLockingService.class})
@TestPropertySource(properties = {
        "optimistic-locking.retry.max-attempts=3",
        "optimistic-locking.retry.backoff-delay-ms=1",
        "optimistic-locking.retry.backoff-multiplier=1.0"
})
class OptimisticLockingRetryAnnotationIntegrationTest {

    @Autowired
    private TestOptimisticLockingService service;

    @BeforeEach
    void resetCounters() {
        service.resetCounters();
    }

    @Test
    void shouldRetryOnOptimisticLockingExceptionUntilSuccess() {
        service.retryOnOptimisticLockingUntilSuccess();
        assertEquals(3, service.getOptimisticCounter());
    }

    @Test
    void shouldNotRetryForNonOptimisticExceptions() {
        assertThrows(IllegalStateException.class, () -> service.noRetryOnOtherException());
        assertEquals(1, service.getNonOptimisticCounter());
    }

    @Test
    void shouldStopAfterConfiguredMaxAttemptsWhenStillFailing() {
        assertThrows(Exception.class, () -> service.alwaysThrowOptimisticLockingFailure());
        // max-attempts=3 means 3 total invocations
        assertEquals(3, service.getOptimisticCounter());
    }

    @Test
    void shouldUseAnnotationOverridesForMaxAttempts() {
        service.customConfigRetry();
        assertEquals(3, service.getCustomCounter());
    }
}
