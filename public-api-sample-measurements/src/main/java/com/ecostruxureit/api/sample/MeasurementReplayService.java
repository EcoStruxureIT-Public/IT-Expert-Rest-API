/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import generated.dto.Measurement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MeasurementReplayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementReplayService.class);

    private final MeasurementRepository measurementRepository;

    private final ReplayRepository replayRepository;

    MeasurementReplayService(MeasurementRepository measurementRepository, ReplayRepository replayRepository) {

        this.measurementRepository = Objects.requireNonNull(measurementRepository);
        this.replayRepository = Objects.requireNonNull(replayRepository);
    }

    @Transactional
    public Optional<String> getLatestOffset() {

        Replay replay = replayRepository.findWhereToOffsetIsEmpty();

        if (replay == null) {
            return Optional.empty();
        }

        return Optional.of(replay.getFromOffset());
    }

    @Transactional
    public List<Replay> findPendingReplays() {

        return replayRepository.findWhereToOffsetIsNotEmpty();
    }

    @Transactional
    public void updateReplays(String offset, boolean offsetHasBeenReceivedPreviously) {

        Replay potentialReplay = replayRepository.findWhereToOffsetIsEmpty();

        if (potentialReplay == null) {
            // The first time the application is started (or if the database is not persisted), we only need to start
            // remembering the offset
            // we have just received so that it may be used as the "from offset" for a potential replay in the future.
            insertPotentialReplay(offset);
            return;
        }

        if (!offsetHasBeenReceivedPreviously) {
            // The first time we receive an offset, we need to register a pending replay using the previously saved
            // offset as the
            // "from offset" and the latest received offset as the "to offset".
            Replay pendingReplay = new Replay(potentialReplay.getFromOffset(), offset);
            LOGGER.debug("Adding pending replay: {}", pendingReplay);
            replayRepository.insert(pendingReplay);
        }

        // Update the latest offset we have received so that it may be used as the "from offset" for a potential replay
        // in the future.
        replayRepository.delete(potentialReplay);
        insertPotentialReplay(offset);
    }

    @Transactional
    public Replay saveMeasurementsAndMaybeUpdateReplay(List<Measurement> measurements, Replay replay, String offset) {

        LOGGER.debug("Adding {} measurement(s)", measurements.size());

        measurementRepository.batchInsertOrUpdate(measurements);

        if (offset == null) {
            return replay;
        }

        // Since a new offset has been received, we can create a new (smaller) pending replay and remove the previous
        // (larger) one. This
        // means that progress is not lost if a replay has been aborted.

        Replay updatedReplay = new Replay(offset, replay.getToOffset());

        LOGGER.debug("Updating pending replay {} to {}", replay, updatedReplay);

        replayRepository.delete(replay);
        replayRepository.insert(updatedReplay);

        return updatedReplay;
    }

    @Transactional
    public void saveMeasurementsAndDeleteReplay(List<Measurement> measurements, Replay replay) {

        LOGGER.debug("Adding {} measurement(s)", measurements.size());

        measurementRepository.batchInsertOrUpdate(measurements);

        LOGGER.debug("Removing pending replay {}", replay);

        replayRepository.delete(replay);
    }

    @Transactional
    public void delete(Replay replay) {

        LOGGER.debug("Removing pending replay {}", replay);

        replayRepository.delete(replay);
    }

    private void insertPotentialReplay(String fromOffset) {

        LOGGER.debug("Updating latest offset: {}", fromOffset);
        Replay potentialReplay = new Replay(fromOffset, "");
        replayRepository.insert(potentialReplay);
    }
}
