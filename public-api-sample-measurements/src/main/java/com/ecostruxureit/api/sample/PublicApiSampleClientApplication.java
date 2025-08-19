/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
public class PublicApiSampleClientApplication {

    public static void main(String[] args) {

        SpringApplication.run(PublicApiSampleClientApplication.class, args);
    }

    // Note that ExecutorServiceAdapter does not support the lifecycle methods of ExecutorService.
    @Bean(destroyMethod = "")
    ExecutorService executorService(TaskExecutor taskExecutor) {

        return new ExecutorServiceAdapter(taskExecutor);
    }

    @Bean
    TaskExecutor taskExecutor() {

        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

        threadPoolTaskExecutor.setCorePoolSize(3);
        threadPoolTaskExecutor.setMaxPoolSize(3);

        return threadPoolTaskExecutor;
    }

    @Bean
    Duration sleepBetweenFetchesDuration() {

        return Duration.ofSeconds(10);
    }

    @Bean
    Consumer<Throwable> fatalExceptionConsumer() {

        return throwable -> System.exit(1);
    }
}
