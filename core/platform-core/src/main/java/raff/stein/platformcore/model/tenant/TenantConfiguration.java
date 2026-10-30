package raff.stein.platformcore.model.tenant;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the WMP tenant resolver with Hibernate (discriminator-based multi-tenancy via {@code @TenantId}).
 */
@Configuration
@ConditionalOnClass(EntityManagerFactory.class)
public class TenantConfiguration {

    @Bean
    public HibernatePropertiesCustomizer wmpTenantIdentifierResolverCustomizer() {
        final WmpTenantIdentifierResolver resolver = new WmpTenantIdentifierResolver();
        return hibernateProperties -> hibernateProperties.put(
                MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                resolver);
    }
}
