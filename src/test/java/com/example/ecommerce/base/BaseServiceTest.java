package com.example.ecommerce.base;

import com.example.ecommerce.config.TestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for all unit tests.
 * Uses H2 in-memory database via application-test.properties.
 * Includes TestConfig which provides mock beans.
 *
 * Usage:
 * @SpringBootTest
 * class MyServiceTest extends BaseServiceTest { ... }
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(MockitoExtension.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
public abstract class BaseServiceTest {

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
