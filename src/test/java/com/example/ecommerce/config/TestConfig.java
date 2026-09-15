package com.example.ecommerce.config;

import com.example.ecommerce.service.UserDetailServiceCustom;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestConfig {

    /**
     * Mocked RedisConnectionFactory for unit tests.
     * Prevents actual Redis connection attempts during testing.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    /**
     * Real PasswordEncoder for testing - needed for security operations.
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Mocked UserDetailServiceCustom for unit tests.
     * Can be customized per test using Mockito.when().
     */
    @Bean
    @Primary
    public UserDetailServiceCustom userDetailServiceCustom() {
        return Mockito.mock(UserDetailServiceCustom.class);
    }
}
