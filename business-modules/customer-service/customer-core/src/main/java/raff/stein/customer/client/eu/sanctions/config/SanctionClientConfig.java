package raff.stein.customer.client.eu.sanctions.config;

import feign.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.security.OAuth2AccessTokenInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

public class SanctionClientConfig {

    @Bean
    public OAuth2AccessTokenInterceptor oAuth2AccessTokenInterceptor(
            @Value("${spring.application.name}") String appName,
            @Value("${spring.application.rest.client.sanction.token.url}") String tokenUri,
            @Value("${spring.application.rest.client.sanction.token.client-id}") String clientId,
            @Value("${spring.application.rest.client.sanction.token.client-secret}") String clientSecret) {

        final ClientRegistration clientRegistration = ClientRegistration
                // the registration ID will be our app name
                .withRegistrationId(appName)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .tokenUri(tokenUri)
                .build();

        // construct the repository with our client registration
        final InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository =
                new InMemoryClientRegistrationRepository(clientRegistration);

        // use the service in order to store the client registration in memory
        final InMemoryOAuth2AuthorizedClientService inMemoryOAuth2AuthorizedClientService =
                new InMemoryOAuth2AuthorizedClientService(inMemoryClientRegistrationRepository);

        return new OAuth2AccessTokenInterceptor(
                appName,
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        inMemoryClientRegistrationRepository,
                        inMemoryOAuth2AuthorizedClientService)
        );
    }

    @Bean
    public Logger.Level sanctionClientLoggerLevel() {return Logger.Level.FULL;}
}
