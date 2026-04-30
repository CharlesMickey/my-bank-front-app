package ru.yandex.practicum.mybank.gateway;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=*"
})
@AutoConfigureObservability
@AutoConfigureWebTestClient
class GatewayServiceApplicationTests {
    private static final Logger TEST_LOGGER = (Logger) LoggerFactory.getLogger(GatewayServiceApplicationTests.class);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private Tracer tracer;

    @Test
    @DisplayName("Actuator health доступен без токена")
    void actuatorHealthIsPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Actuator prometheus доступен без токена")
    void actuatorPrometheusIsPublic() {
        webTestClient.get()
                .uri("/actuator/prometheus")
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

    @Test
    @DisplayName("Trace id и span id попадают в MDC логов")
    void traceIdsAreAddedToMdc() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        TEST_LOGGER.addAppender(appender);

        var span = tracer.nextSpan().name("gateway-mdc-test").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            TEST_LOGGER.info("gateway mdc test");
        } finally {
            span.end();
            TEST_LOGGER.detachAppender(appender);
            appender.stop();
        }

        ILoggingEvent event = appender.list.get(appender.list.size() - 1);
        assertThat(event.getMDCPropertyMap().get("traceId")).isNotBlank();
        assertThat(event.getMDCPropertyMap().get("spanId")).isNotBlank();
    }
}
