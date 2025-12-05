package raff.stein.platformcore.async;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the shared platform {@code ThreadPoolTaskExecutor}.
 * <p>
 * Properties are bound from the {@code platform.async.task-executor} prefix and
 * control core pool sizing, queue capacity, thread naming, rejection policy and
 * shutdown behavior. Business microservices can override these values in their
 * own {@code application-*.yml} files.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "platform.async.task-executor")
public class AsyncTaskExecutorProperties {

    /**
     * Minimum number of threads kept in the pool, even if idle.
     */
    private int corePoolSize = 10;

    /**
     * Maximum number of threads allowed in the pool.
     */
    private int maxPoolSize = 50;

    /**
     * Maximum number of tasks that can be queued before new submissions are rejected
     * according to {@link RejectionPolicy}.
     */
    private int queueCapacity = 1000;

    /**
     * Time in seconds that excess idle threads will wait for new tasks before terminating.
     */
    private int keepAliveSeconds = 60;

    /**
     * Prefix for thread names created by the executor, useful for log filtering and debugging.
     */
    private String threadNamePrefix = "platform-async-";

    /**
     * Policy applied when the pool and queue are saturated.
     */
    private RejectionPolicy rejectionPolicy = RejectionPolicy.CALLER_RUNS;

    /**
     * Whether the executor should wait for running tasks to complete on application shutdown.
     */
    private boolean waitForTasksToCompleteOnShutdown = true;

    /**
     * Maximum number of seconds to wait for tasks to finish on shutdown when
     * {@link #waitForTasksToCompleteOnShutdown} is enabled.
     */
    private int awaitTerminationSeconds = 30;

    /**
     * Supported rejection policies for the shared executor.
     */
    public enum RejectionPolicy {
        /**
         * Execute the task in the calling thread when the pool is saturated (provides backpressure).
         */
        CALLER_RUNS,
        /**
         * Throw {@code RejectedExecutionException} when the pool is saturated.
         */
        ABORT,
        /**
         * Silently discard the new task when the pool is saturated.
         */
        DISCARD,
        /**
         * Discard the oldest queued task and enqueue the new one when the pool is saturated.
         */
        DISCARD_OLDEST
    }
}
