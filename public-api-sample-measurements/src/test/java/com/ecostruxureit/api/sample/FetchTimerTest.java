package com.ecostruxureit.api.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.ecostruxureit.api.sample.client.ApiException;
import com.ecostruxureit.api.sample.client.RetriableApiException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
@ExtendWith(MockitoExtension.class)
class FetchTimerTest {

    private static final Duration SLEEP_BETWEEN_FETCHES = Duration.ZERO;

    private final FatalExceptionConsumer fatalExceptionConsumer = new FatalExceptionConsumer();

    @Mock
    private FetchEngine fetchEngine;

    @Test
    void whenErrorIsThrown_thenFatalExceptionConsumerIsCalledWithError() throws ApiException {

        // Given

        ForceExitError forceExitError = new ForceExitError();

        doThrow(forceExitError).when(fetchEngine).fetch();

        FetchTimer fetchTimer = createFetchTimer();

        // When

        fetchTimer.onApplicationReadyEvent();

        // Then

        verifyFetchEngineInvocations(1);

        verifyFatalExceptionConsumer(forceExitError);
    }

    @Test
    void whenRetriableApiExceptionIsThrown_thenFetchesAgain() throws ApiException {

        // Given

        // Note: Throwing ForceExitError on second call to force FetchTimer to stop running.

        ForceExitError forceExitError = new ForceExitError();

        doThrow(new SomeRetriableApiException())
                .doThrow(forceExitError)
                .when(fetchEngine)
                .fetch();

        FetchTimer fetchTimer = createFetchTimer();

        // When

        fetchTimer.onApplicationReadyEvent();

        // Then

        verifyFetchEngineInvocations(2);

        verifyFatalExceptionConsumer(forceExitError);
    }

    private FetchTimer createFetchTimer() {

        return new FetchTimer(fetchEngine, new SyncTaskExecutor(), SLEEP_BETWEEN_FETCHES, fatalExceptionConsumer);
    }

    private void verifyFetchEngineInvocations(int invocations) throws ApiException {

        verify(fetchEngine, times(invocations)).fetch();

        verifyNoMoreInteractions(fetchEngine);
    }

    private void verifyFatalExceptionConsumer(Throwable... throwables) {

        assertThat(fatalExceptionConsumer.throwables).containsExactly(throwables);
    }

    private static final class FatalExceptionConsumer implements Consumer<Throwable> {

        final List<Throwable> throwables = new ArrayList<>();

        @Override
        public void accept(Throwable throwable) {

            throwables.add(throwable);
        }
    }

    private static final class SomeRetriableApiException extends RetriableApiException {

        SomeRetriableApiException() {

            super("Some retriable API exception");
        }
    }

    private static final class ForceExitError extends Error {}
}
