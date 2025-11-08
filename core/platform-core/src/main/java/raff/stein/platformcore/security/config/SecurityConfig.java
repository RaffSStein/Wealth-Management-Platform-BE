package raff.stein.platformcore.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import raff.stein.platformcore.security.context.SecurityContextFilter;
import raff.stein.platformcore.security.error.RestAccessDeniedHandler;

import java.util.List;

/**
 * Central security configuration enabling:
 * - Stateless sessions and OAuth2 Resource Server with JWT
 * - Custom filter chain ordering and matchers
 * - URL authorization rules + method security
 * - CSRF disabled for stateless APIs
 * - CORS configuration
 * - Custom authority conversion strategy
 * - Custom authentication entrypoint and access denied handler
 * - Exposure of AuthenticationManager, UserDetailsService, PasswordEncoder
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Main security filter chain with ordered filters and endpoint rules.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextFilter securityContextFilter,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                // We build a stateless API
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Disable CSRF for stateless APIs
                .csrf(AbstractHttpConfigurer::disable)
                // Enable CORS with a bean-defined configuration
                .cors(Customizer.withDefaults())
                // Exception handling customization
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // Define which endpoints are public vs protected
                .authorizeHttpRequests(auth -> auth
                        // Example public endpoints for auth (login/register)
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        // Actuator health/info could be public depending on needs
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                // Resource server with JWT support; customize authorities mapping
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );

        // Register the custom context filter before authorization takes place
        http.addFilterBefore(securityContextFilter, org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configure how roles/authorities are extracted from the JWT.
     * This converter supports a "roles" claim mapped to ROLE_*
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        // If authorities are stored under a different claim, set it here
        gac.setAuthoritiesClaimName("roles");
        // Prefix with ROLE_ so that hasRole("X") works
        gac.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(gac);
        return conv;
    }

    /**
     * AccessDeniedHandler that returns 403 with a minimal JSON body.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new RestAccessDeniedHandler();
    }

    /**
     * Minimal CORS configuration allowing common methods and headers.
     * Tighten origins and headers according to the environment.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID"));
        config.setExposedHeaders(List.of("X-Correlation-ID"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Password encoder for application credentials.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expose AuthenticationManager built from the configured AuthenticationProviders.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Placeholder UserDetailsService only used if a service module does NOT provide its own.
     */
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UnsupportedOperationException("UserDetailsService not implemented in platform-core. Provide a bean in your service module.");
        };
    }
}
