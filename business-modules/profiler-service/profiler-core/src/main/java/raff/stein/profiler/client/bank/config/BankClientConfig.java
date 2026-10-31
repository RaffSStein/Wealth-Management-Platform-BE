package raff.stein.profiler.client.bank.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;

/**
 * Not a @Configuration on purpose: it applies to the bank client only.
 */
public class BankClientConfig {

    // bank-service is a resource server too: call it on behalf of the current user
    @Bean
    public RequestInterceptor bankClientBearerForwarder() {
        return template -> {
            final WMPContext context = SecurityContextHolder.getContext();
            if (context != null && context.getRawToken() != null) {
                template.header("Authorization", "Bearer " + context.getRawToken());
            }
        };
    }
}
