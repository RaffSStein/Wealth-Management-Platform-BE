package raff.stein.platformcore.async;

import io.micrometer.observation.ObservationRegistry;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.context.ActiveProfiles;
import raff.stein.platformcore.bean.PlatformCoreBeans;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {PlatformCoreBeans.class, AsyncTaskExecutorConfigurationTest.TestConfig.class})
@ActiveProfiles("test")
class AsyncTaskExecutorConfigurationTest {

    @Autowired
    private TaskExecutor platformTaskExecutor;

    @Autowired
    private TestAsyncService testAsyncService;

    @Test
    void shouldCreatePlatformTaskExecutorBean() {
        assertThat(platformTaskExecutor).isNotNull();
    }

    @Test
    void shouldPropagateExecutionToAsyncMethod() throws InterruptedException {
        String payload = UUID.randomUUID().toString();
        testAsyncService.executeAsync(payload);

        String received = testAsyncService.getQueue().take();
        assertThat(received).isEqualTo(payload);
    }

    @Configuration
    static class TestConfig {

        @Bean
        ObservationRegistry observationRegistry() {
            return ObservationRegistry.create();
        }

        @Bean
        TestAsyncService testAsyncService() {
            return new TestAsyncService();
        }
    }

    @Getter
    static class TestAsyncService {

        private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);

        @Async("platformTaskExecutor")
        public void executeAsync(String payload) {
            queue.add(payload);
        }
    }
}
