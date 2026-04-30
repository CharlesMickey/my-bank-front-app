package ru.yandex.practicum.mybank.transfer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=*"
})
@AutoConfigureObservability
@AutoConfigureMockMvc
class TransferServiceApplicationTests {
    private static final Logger TEST_LOGGER = (Logger) LoggerFactory.getLogger(TransferServiceApplicationTests.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tracer tracer;

    @Test
    @DisplayName("Actuator health доступен без аутентификации")
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Actuator prometheus доступен без аутентификации")
    void actuatorPrometheusIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sensitive actuator endpoint требует аутентификацию")
    void actuatorEnvRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Trace id и span id попадают в MDC логов")
    void traceIdsAreAddedToMdc() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        TEST_LOGGER.addAppender(appender);

        var span = tracer.nextSpan().name("transfer-mdc-test").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            TEST_LOGGER.info("transfer mdc test");
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
