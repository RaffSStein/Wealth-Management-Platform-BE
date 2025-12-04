package raff.stein.platformcore.optimisticlocking;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import raff.stein.platformcore.optimisticlocking.config.OptimisticLockingRetryConfiguration;

@Configuration
@Import(OptimisticLockingRetryConfiguration.class)
class OptimisticLockingRetryTestConfiguration {
}
