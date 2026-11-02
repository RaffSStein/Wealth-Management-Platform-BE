package raff.stein.platformcore.messaging.outbox;

import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.producer.Producer;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

/**
 * Turns the transactional outbox on with {@code platform.outbox.enabled=true}. It needs a datasource.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
@ConditionalOnProperty(name = "platform.outbox.enabled", havingValue = "true")
@Import(OutboxConfiguration.OutboxPackageRegistrar.class)
public class OutboxConfiguration {

    @Bean(destroyMethod = "shutdown")
    public OutboxRelay outboxRelay(final OutboxEventRepository repository,
                                   final Producer<String, CloudEvent> kafkaCloudEventProducer,
                                   final OutboxProperties properties,
                                   final PlatformTransactionManager transactionManager) {
        return new OutboxRelay(repository, kafkaCloudEventProducer, properties, transactionManager, Clock.systemUTC());
    }

    @Bean
    public OutboxEventWriter outboxEventWriter(final OutboxEventRepository repository, final OutboxRelay relay) {
        return new OutboxEventWriter(repository, relay, Clock.systemUTC());
    }

    /**
     * A service scans only its own package for entities and repositories. This adds the outbox package to the same
     * list. {@code @EntityScan} would replace the list instead, and hide the service's own entities.
     */
    static class OutboxPackageRegistrar implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(final @NonNull AnnotationMetadata importingClassMetadata,
                                            final @NonNull BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, OutboxEventEntity.class.getPackageName());
        }
    }
}
