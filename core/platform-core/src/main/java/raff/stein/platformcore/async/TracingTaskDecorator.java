package raff.stein.platformcore.async;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.core.task.TaskDecorator;

/**
 * {@link TaskDecorator} that wraps asynchronous executions in a Micrometer {@link Observation}.
 * <p>
 * This ensures that work executed on the shared {@code platformTaskExecutor} participates in
 * the platform's observability model, allowing logs and metrics produced inside {@code @Async}
 * methods to be correlated with the rest of the request or workflow.
 */
public class TracingTaskDecorator implements TaskDecorator {

    private final ObservationRegistry observationRegistry;

    /**
     * Create a new decorator bound to the given {@link ObservationRegistry}.
     *
     * @param observationRegistry the observation registry used to start observations
     */
    public TracingTaskDecorator(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        Observation parent = Observation.start("async.task", observationRegistry);
        return () -> {
            try (Observation.Scope scope = parent.openScope()) {
                runnable.run();
            }
        };
    }
}
