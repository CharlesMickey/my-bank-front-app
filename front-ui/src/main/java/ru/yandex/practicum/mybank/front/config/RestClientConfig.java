package ru.yandex.practicum.mybank.front.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FrontProperties.class)
public class RestClientConfig {

    @Bean
    RestClient gatewayRestClient(RestClient.Builder builder, FrontProperties properties) {
        return builder.baseUrl(properties.gatewayUrl()).build();
    }
}
