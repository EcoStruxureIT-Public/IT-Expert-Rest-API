package com.ecostruxureit.api.sample;

import generated.dto.Measurement;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
@Service
class MeasurementLiveService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementLiveService.class);

    private final MeasurementRepository measurementRepository;

    MeasurementLiveService(MeasurementRepository measurementRepository) {

        this.measurementRepository = Objects.requireNonNull(measurementRepository);
    }

    @Transactional
    public void saveMeasurements(List<Measurement> measurements) {

        LOGGER.debug("Adding {} measurement(s)", measurements.size());

        measurementRepository.batchInsertOrUpdate(measurements);
    }
}
