/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import com.ecostruxureit.api.sample.client.RetriableApiException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Continuously asks {@link FetchEngine} to retrieve measurements from the REST API.
 * <p>
 * Note that we don't create an instance of this class when running tests as it would interfere with our tests if it starts a thread that
 * fetches data on its own. See {@link Profiles} for more info.
 */
@Service
@Profile(Profiles.NOT_TEST)
public class FetchTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchTimer.class);

    private final BackOff backOff;

    private final FetchEngine fetchEngine;

    private final TaskExecutor taskExecutor;

    private final long sleepBetweenFetchesInMilliseconds;

    private final Consumer<Throwable> fatalExceptionConsumer;

    FetchTimer(
            FetchEngine fetchEngine,
            TaskExecutor taskExecutor,
            Duration sleepBetweenFetchesDuration,
            Consumer<Throwable> fatalExceptionConsumer) {

        this.backOff = createBackOff(sleepBetweenFetchesDuration.toMillis());
        this.fetchEngine = Objects.requireNonNull(fetchEngine);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.sleepBetweenFetchesInMilliseconds = sleepBetweenFetchesDuration.toMillis();
        this.fatalExceptionConsumer = Objects.requireNonNull(fatalExceptionConsumer);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent() {

        taskExecutor.execute(() -> {
            BackOffExecution backOffExecution = null;

            while (true) {

                try {
                    fetchEngine.fetch();
                    backOffExecution = null;
                } catch (RetriableApiException ex) {
                    if (backOffExecution == null) {
                        backOffExecution = backOff.start();
                    }
                    LOGGER.error("Caught retriable exception, retrying later", ex);
                } catch (Throwable ex) {
                    LOGGER.error("Caught exception, shutting down", ex);
                    fatalExceptionConsumer.accept(ex);
                    break;
                }

                long sleepInMilliseconds =
                        backOffExecution == null ? sleepBetweenFetchesInMilliseconds : backOffExecution.nextBackOff();

                try {
                    Thread.sleep(sleepInMilliseconds);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    private static BackOff createBackOff(long initialIntervalInMilliseconds) {

        ExponentialBackOff exponentialBackOff = new ExponentialBackOff(initialIntervalInMilliseconds, 2);
        exponentialBackOff.setMaxInterval(TimeUnit.MINUTES.toMillis(10));
        return exponentialBackOff;
    }
}
