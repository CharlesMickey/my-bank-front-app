package ru.yandex.practicum.mybank.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class TransferServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
