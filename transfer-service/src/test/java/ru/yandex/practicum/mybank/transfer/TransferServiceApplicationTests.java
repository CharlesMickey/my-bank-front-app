package ru.yandex.practicum.mybank.transfer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=*"
})
@AutoConfigureMockMvc
class TransferServiceApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Actuator health доступен без аутентификации")
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sensitive actuator endpoint требует аутентификацию")
    void actuatorEnvRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }
}
