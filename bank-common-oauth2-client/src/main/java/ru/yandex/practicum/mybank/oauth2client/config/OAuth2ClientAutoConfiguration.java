package ru.yandex.practicum.mybank.oauth2client.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
public class OAuth2ClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "loadBalancedWebClientBuilder")
    WebClient.Builder loadBalancedWebClientBuilder(ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        WebClient.Builder builder = WebClient.builder();
        ObservationRegistry observationRegistry = observationRegistryProvider.getIfAvailable();
        if (observationRegistry != null) {
            builder.observationRegistry(observationRegistry);
        }
        return builder;
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService
                );
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean(name = "serviceWebClient")
    WebClient serviceWebClient(
            WebClient.Builder loadBalancedWebClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2.setDefaultClientRegistrationId("bank-service");
        return loadBalancedWebClientBuilder.apply(oauth2.oauth2Configuration()).build();
    }
}
