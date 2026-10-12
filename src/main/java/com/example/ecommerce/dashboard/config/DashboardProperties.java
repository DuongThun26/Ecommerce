package com.example.ecommerce.dashboard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class DashboardProperties {

    private Executors executors = new Executors();
    private Dashboard dashboard = new Dashboard();

    @Data
    public static class Executors {
        private DashboardExecutor dashboard = new DashboardExecutor();
    }

    @Data
    public static class DashboardExecutor {
        private int coreSize = 4;
        private int maxSize = 4;
        private int queueCapacity = 50;
    }

    @Data
    public static class Dashboard {
        private Duration partTimeout = Duration.ofMillis(1500);
    }
}
