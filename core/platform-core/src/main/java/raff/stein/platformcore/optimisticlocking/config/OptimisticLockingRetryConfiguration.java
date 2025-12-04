package raff.stein.platformcore.optimisticlocking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
@EnableConfigurationProperties(OptimisticLockingRetryProperties.class)
public class OptimisticLockingRetryConfiguration {

}

