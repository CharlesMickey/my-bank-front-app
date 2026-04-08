package ru.yandex.practicum.mybank.cash;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class CashServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
