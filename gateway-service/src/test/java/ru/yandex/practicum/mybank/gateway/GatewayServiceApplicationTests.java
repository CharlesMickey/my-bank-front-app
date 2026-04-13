package ru.yandex.practicum.mybank.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=*"
})
@AutoConfigureWebTestClient
class GatewayServiceApplicationTests {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Actuator health доступен без токена")
    void actuatorHealthIsPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Sensitive actuator endpoint требует токен")
    void actuatorEnvRequiresAuthentication() {
        webTestClient.get()
                .uri("/actuator/env")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
