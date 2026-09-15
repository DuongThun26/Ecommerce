package com.example.ecommerce.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers configuration for integration tests.
 * Provides a managed PostgreSQL container for tests.
 *
 * Usage in test classes:
 * @SpringBootTest
 * @Import(TestContainersConfig.class)
 * class MyIntegrationTest { ... }
 */
@TestConfiguration
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("ecommerce_test")
                .withUsername("postgres")
                .withPassword("postgres")
                .withReuse(true);  // Reuse container across test runs for faster execution
    }
}
