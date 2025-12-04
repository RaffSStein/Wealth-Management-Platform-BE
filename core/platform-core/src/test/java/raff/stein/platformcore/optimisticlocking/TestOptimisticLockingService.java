package raff.stein.platformcore.optimisticlocking;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import raff.stein.platformcore.optimisticlocking.annotation.OptimisticLockingRetry;

import java.util.concurrent.atomic.AtomicInteger;

@Service
class TestOptimisticLockingService {

    private final AtomicInteger optimisticCounter = new AtomicInteger();
    private final AtomicInteger nonOptimisticCounter = new AtomicInteger();
    private final AtomicInteger customCounter = new AtomicInteger();

    @OptimisticLockingRetry
    void retryOnOptimisticLockingUntilSuccess() {
        int current = optimisticCounter.getAndIncrement();
        if (current < 2) {
            throw new ObjectOptimisticLockingFailureException("TestEntity", 1L);
        }
    }

    @OptimisticLockingRetry
    void alwaysThrowOptimisticLockingFailure() {
        optimisticCounter.incrementAndGet();
        throw new ObjectOptimisticLockingFailureException("TestEntity", 1L);
    }

    @OptimisticLockingRetry
    void noRetryOnOtherException() {
        nonOptimisticCounter.incrementAndGet();
        throw new IllegalStateException("non optimistic failure");
    }

    @OptimisticLockingRetry(
            maxAttemptsExpression = "3",
            backoff = @Backoff(delay = 1, multiplier = 1.0)
    )
    void customConfigRetry() {
        int current = customCounter.getAndIncrement();
        if (current < 2) {
            throw new OptimisticLockingFailureException("conflict");
        }
    }

    int getOptimisticCounter() {
        return optimisticCounter.get();
    }

    int getNonOptimisticCounter() {
        return nonOptimisticCounter.get();
    }

    int getCustomCounter() {
        return customCounter.get();
    }

    void resetCounters() {
        optimisticCounter.set(0);
        nonOptimisticCounter.set(0);
        customCounter.set(0);
    }
}
