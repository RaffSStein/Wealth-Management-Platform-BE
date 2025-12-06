package raff.stein.platformcore.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that demonstrate and verify platform-level cache behavior
 * using @Cacheable and @CacheEvict with the SIMPLE_IN_MEMORY provider.
 */
@SpringBootTest(classes = PlatformCacheDemoServiceTest.TestConfig.class)
@TestPropertySource(properties = {
        "platform.cache.enabled=true",
        "platform.cache.provider=SIMPLE_IN_MEMORY",
        "platform.cache.caches.demoCache.ttl=5m"
})
class PlatformCacheDemoServiceTest {

    @Configuration
    @EnableCaching
    @Import({ PlatformCacheConfiguration.class, PlatformCacheDemoService.class })
    @ImportAutoConfiguration
    static class TestConfig {
        // imports platform cache configuration and demo service into the test context
    }

    @Autowired
    private PlatformCacheDemoService platformCacheDemoService;

    @BeforeEach
    void setUp() {
        platformCacheDemoService.resetInvocationCount();
    }

    @Test
    void shouldUseCacheOnSecondInvocation() {
        String first = platformCacheDemoService.loadValue("key-1");
        String cached = platformCacheDemoService.loadValue("key-1");

        assertThat(first).isEqualTo(cached);
        assertThat(platformCacheDemoService.getInvocationCount()).isEqualTo(1);
    }

    @Test
    void shouldEvictCachedEntry() {
        String first = platformCacheDemoService.loadValue("key-2");
        String cached = platformCacheDemoService.loadValue("key-2");
        assertThat(first).isEqualTo(cached);
        assertThat(platformCacheDemoService.getInvocationCount()).isEqualTo(1);

        platformCacheDemoService.evictValue("key-2");

        String afterEvict = platformCacheDemoService.loadValue("key-2");
        assertThat(afterEvict).isNotEqualTo(first);
        assertThat(platformCacheDemoService.getInvocationCount()).isEqualTo(2);
    }
}
