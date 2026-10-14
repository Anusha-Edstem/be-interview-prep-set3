package com.interviewprep.booking.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class BookingAsyncConfig {

    public static final String NOTIFICATION_EXECUTOR = "bookingNotificationExecutor";

    private static final int CORE_THREADS = 2;
    private static final int MAX_THREADS = 4;
    private static final int QUEUE_CAPACITY = 500;
    private static final int SHUTDOWN_WAIT_SECONDS = 10;

    @Bean(NOTIFICATION_EXECUTOR)
    public ThreadPoolTaskExecutor bookingNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("booking-notify-");
        executor.setCorePoolSize(CORE_THREADS);
        executor.setMaxPoolSize(MAX_THREADS);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SHUTDOWN_WAIT_SECONDS);
        return executor;
    }
}
