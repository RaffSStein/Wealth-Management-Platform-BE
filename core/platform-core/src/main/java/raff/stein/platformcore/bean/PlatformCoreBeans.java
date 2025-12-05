package raff.stein.platformcore.bean;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import raff.stein.platformcore.async.AsyncTaskExecutorProperties;
import raff.stein.platformcore.async.TracingTaskDecorator;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Central Spring configuration for shared platform beans.
 * <p>
 * The {@link EnableAsync} annotation enables Spring's {@code @Async} processing in any
 * application that includes {@code platform-core} on the classpath.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(AsyncTaskExecutorProperties.class)
public class PlatformCoreBeans {

    /**
     * Shared {@link ThreadPoolTaskExecutor} used for {@code @Async} methods across business modules.
     * <p>
     * It is exposed with the bean name {@code platformTaskExecutor} so that services can explicitly
     * opt in via {@code @Async("platformTaskExecutor")} and configure its sizing and behavior via
     * {@link AsyncTaskExecutorProperties}.
     *
     * @param properties          configuration properties bound from {@code platform.async.task-executor.*}
     * @param observationRegistry Micrometer observation registry used to decorate tasks for tracing
     * @return the configured shared task executor
     */
    @Bean(name = "platformTaskExecutor")
    public TaskExecutor platformTaskExecutor(
            AsyncTaskExecutorProperties properties,
            ObservationRegistry observationRegistry) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(properties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());

        switch (properties.getRejectionPolicy()) {
            case ABORT -> executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
            case DISCARD -> executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
            case DISCARD_OLDEST -> executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
            case CALLER_RUNS -> executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        }

        executor.setTaskDecorator(new TracingTaskDecorator(observationRegistry));
        executor.initialize();
        return executor;
    }
}
