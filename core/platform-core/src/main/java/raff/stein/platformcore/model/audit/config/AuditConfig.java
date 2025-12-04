package raff.stein.platformcore.model.audit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import raff.stein.platformcore.exception.types.unauthorized.WmpContextException;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
@Slf4j
public class AuditConfig {


    @Bean
    public AuditorAware<String> auditorProvider() {
        return () ->  Optional.of(getCurrentUserEmail());
    }

    /**
     * Retrieve the current user's email from the Spring SecurityContext.
     * @return the email of the authenticated user, or ANONYMOUS if not available
     */
    private String getCurrentUserEmail() {
        WMPContext wmpContext = SecurityContextHolder.getContext();
        if (wmpContext != null) {
            String userEmail = wmpContext.getEmail();
            if (userEmail != null && !userEmail.isBlank()) {
                return userEmail;
            } else {
                // Throws WmpContextException if userEmail is null or blank
                throw WmpContextException.forMissingField("userEmail").get();
            }
        }
        log.warn("WMPContext is null. Returning 'ANONYMOUS' as auditor.");
        return "ANONYMOUS";
    }
}
