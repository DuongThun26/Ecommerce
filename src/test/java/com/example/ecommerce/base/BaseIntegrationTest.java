package com.example.ecommerce.base;

import com.example.ecommerce.config.TestConfig;
import com.example.ecommerce.config.TestContainersConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for full end-to-end integration tests.
 * Uses Testcontainers for real database, MockMvc for HTTP testing.
 * Full application context loaded.
 *
 * Usage:
 * @SpringBootTest
 * @Import(TestContainersConfig.class)
 * class MyIntegrationTest extends BaseIntegrationTest { ... }
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@Import({TestConfig.class, TestContainersConfig.class})
@ActiveProfiles("integrationtest")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Convert object to JSON string.
     */
    protected String asJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Convert JSON string to object.
     */
    protected <T> T fromJson(String json, Class<T> type) throws Exception {
        return objectMapper.readValue(json, type);
    }
}
