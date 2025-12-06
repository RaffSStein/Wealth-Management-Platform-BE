package raff.stein.platformcore.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple demo service to showcase and validate platform-level caching
 * using {@link Cacheable} and {@link CacheEvict}.
 */
@Service
public class PlatformCacheDemoService {

    private final AtomicInteger invocationCounter = new AtomicInteger();

    @Cacheable(cacheNames = "demoCache", key = "#key")
    public String loadValue(String key) {
        return key + "-" + invocationCounter.incrementAndGet();
    }

    @CacheEvict(cacheNames = "demoCache", key = "#key")
    public void evictValue(String key) {
        // eviction handled by the annotation
    }

    int getInvocationCount() {
        return invocationCounter.get();
    }

    void resetInvocationCount() {
        invocationCounter.set(0);
    }
}
