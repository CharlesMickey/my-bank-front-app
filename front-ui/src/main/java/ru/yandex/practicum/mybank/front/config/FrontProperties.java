package ru.yandex.practicum.mybank.front.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank")
public record FrontProperties(String gatewayUrl, String logoutUrl) {

    public FrontProperties {
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            gatewayUrl = "http://localhost:8090";
        }
        if (logoutUrl == null || logoutUrl.isBlank()) {
            logoutUrl = "http://localhost:9090/realms/my-bank/protocol/openid-connect/logout";
        }
    }
}
