/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import com.ecostruxureit.api.sample.client.ApiClient;
import com.ecostruxureit.api.sample.client.ApiException;
import com.ecostruxureit.api.sample.client.InvalidRequestException;
import generated.dto.Measurement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class FetchEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchEngine.class);

    private final ApiClient apiClient;

    private final ExecutorService executorService;

    private final MeasurementLiveService measurementLiveService;

    private final MeasurementReplayService measurementReplayService;

    private final TransactionTemplate transactionTemplate;

    private final boolean replayEnabled;

    private final int batchSize;

    private Future<Void> liveFuture;

    private Future<Void> replayFuture;

    FetchEngine(
            Configuration configuration,
            ApiClient apiClient,
            ExecutorService executorService,
            MeasurementLiveService measurementLiveService,
            MeasurementReplayService measurementReplayService,
            TransactionTemplate transactionTemplate) {

        this.apiClient = Objects.requireNonNull(apiClient);
        this.executorService = Objects.requireNonNull(executorService);
        this.measurementLiveService = Objects.requireNonNull(measurementLiveService);
        this.measurementReplayService = Objects.requireNonNull(measurementReplayService);
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate);

        this.batchSize = configuration.getBatchSize();
        this.replayEnabled = configuration.isReplayEnabled();
    }

    void fetch() throws ApiException {

        maybeRetrieveLiveMeasurements();

        if (replayEnabled) {
            maybeReplayMeasurements();
        }
    }

    private void maybeRetrieveLiveMeasurements() throws ApiException {

        if (liveFuture != null) {
            if (!liveFuture.isDone()) {
                return;
            }
            try {
                maybeRethrowExceptionForDoneFuture(liveFuture);
            } finally {
                liveFuture = null;
            }
        }

        LOGGER.info("Starting to retrieve live measurements");

        liveFuture = executorService.submit(() -> {
            apiClient.retrieveLiveMeasurements(new LiveConsumer());
            return null;
        });
    }

    private void maybeReplayMeasurements() throws ApiException {

        if (replayFuture != null) {
            if (!replayFuture.isDone()) {
                return;
            }
            try {
                maybeRethrowExceptionForDoneFuture(replayFuture);
            } finally {
                replayFuture = null;
            }
        }

        LOGGER.debug("Checking for pending replays");

        List<Replay> replays = measurementReplayService.findPendingReplays();

        if (replays.isEmpty()) {
            LOGGER.debug("No pending replays found");
            return;
        }

        // We are only replaying one interval at a time to minimize the risk of being rate limited.
        Replay replay = replays.get(0);
        String fromOffset = replay.getFromOffset();
        String toOffset = replay.getToOffset();

        LOGGER.info("Starting to replay measurements from {} to {}", fromOffset, toOffset);

        replayFuture = executorService.submit(() -> {
            ReplayConsumer replayConsumer = new ReplayConsumer(replay);

            try {
                apiClient.replayMeasurements(fromOffset, toOffset, replayConsumer);
            } catch (InvalidRequestException ex) {
                LOGGER.error("Failed to replay from {} to {}", fromOffset, toOffset, ex);
                measurementReplayService.delete(replay);
                return null;
            }

            replayConsumer.complete();

            LOGGER.info("Completed replay from {} to {}", fromOffset, toOffset);

            return null;
        });
    }

    private static void maybeRethrowExceptionForDoneFuture(Future<?> future) throws ApiException {

        if (!future.isDone()) {
            return;
        }

        try {
            future.get();
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ApiException exception) {
                throw exception;
            }
            if (cause instanceof RuntimeException exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final class LiveConsumer implements Consumer<Measurement> {

        private final List<Measurement> measurements = new ArrayList<>(batchSize);

        /**
         * Since both a "from offset" and a "to offset" are required to perform a replay, after a disconnect we need to start to retrieve
         * live measurements - and first time we receive an offset we got the "to offset". The "from offset" is known from before the
         * disconnect since we continuously save the latest offset retrieved.
         */
        private boolean offsetHasBeenReceivedPreviously;

        @Override
        public void accept(Measurement measurement) {

            measurements.add(measurement);

            String offset = measurement.getOffset();

            if (offset == null && measurements.size() < batchSize) {
                return;
            }

            transactionTemplate.executeWithoutResult(transactionStatus -> {
                measurementLiveService.saveMeasurements(measurements);

                if (replayEnabled && offset != null) {
                    measurementReplayService.updateReplays(offset, offsetHasBeenReceivedPreviously);
                }
            });

            measurements.clear();

            if (offset != null) {
                offsetHasBeenReceivedPreviously = true;
            }
        }
    }

    private final class ReplayConsumer implements Consumer<Measurement> {

        private final List<Measurement> measurements = new ArrayList<>(batchSize);

        private Replay replay;

        ReplayConsumer(Replay replay) {

            this.replay = Objects.requireNonNull(replay);
        }

        @Override
        public void accept(Measurement measurement) {

            measurements.add(measurement);

            String offset = measurement.getOffset();

            if (offset == null && measurements.size() < batchSize) {
                return;
            }

            replay = measurementReplayService.saveMeasurementsAndMaybeUpdateReplay(measurements, replay, offset);

            measurements.clear();
        }

        void complete() {

            measurementReplayService.saveMeasurementsAndDeleteReplay(measurements, replay);
        }
    }
}
