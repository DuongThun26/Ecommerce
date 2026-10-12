package com.example.ecommerce.dashboard.config;

import com.example.ecommerce.common.logging.MdcTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class DashboardExecutorConfig {

    @Bean(name = "dashboardExecutor")
    public ThreadPoolTaskExecutor dashboardExecutor(DashboardProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getExecutors().getDashboard().getCoreSize());
        executor.setMaxPoolSize(properties.getExecutors().getDashboard().getMaxSize());
        executor.setQueueCapacity(properties.getExecutors().getDashboard().getQueueCapacity());
        executor.setThreadNamePrefix("dashboard-exec-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
