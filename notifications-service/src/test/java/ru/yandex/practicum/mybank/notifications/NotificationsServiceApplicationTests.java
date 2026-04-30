package ru.yandex.practicum.mybank.notifications;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notifications;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE SCHEMA IF NOT EXISTS notifications",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureObservability
@AutoConfigureMockMvc
class NotificationsServiceApplicationTests {
    private static final Logger TEST_LOGGER = (Logger) LoggerFactory.getLogger(NotificationsServiceApplicationTests.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tracer tracer;

    @Test
    void startsWithKafkaConsumerAndPrometheusEndpoint() throws Exception {
        assertThat(applicationContext.containsBean("notificationEventConsumer")).isTrue();
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    void traceIdsAreAddedToMdc() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        TEST_LOGGER.addAppender(appender);

        var span = tracer.nextSpan().name("notifications-mdc-test").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            TEST_LOGGER.info("notifications mdc test");
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
