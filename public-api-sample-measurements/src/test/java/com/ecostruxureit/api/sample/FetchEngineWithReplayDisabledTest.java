/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecostruxureit.api.sample.client.ApiException;
import generated.dto.Measurement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles(Profiles.TEST)
@TestPropertySource(properties = {"replayEnabled=false", "batchSize=" + FetchEngineWithReplayDisabledTest.BATCH_SIZE})
class FetchEngineWithReplayDisabledTest {

    static final int BATCH_SIZE = 100;

    @Autowired
    private FetchEngine fetchEngine;

    @Autowired
    private MeasurementReplayService measurementReplayService;

    @Autowired
    private TestHelper testHelper;

    @TestConfiguration
    static class TestSpringConfiguration {

        @Bean
        @Primary
        TaskExecutor syncTaskExecutor() {

            // Runs tasks on the calling thread which makes tests easier to write and understand.
            // Overrides com.ecostruxureit.api.sample.PublicApiSampleClientApplication#taskExecutor

            return new SyncTaskExecutor();
        }
    }

    @BeforeEach
    void beforeEach() {

        testHelper.truncateTables();
    }

    @Test
    void
            whenEnoughMeasurementsWithoutAnOffsetToFillABatchAreRetrieved_thenMeasurementsAreInsertedAndNoReplaysAreInserted()
                    throws ApiException {

        // Given

        Measurement[] inputMeasurements = new Measurement[BATCH_SIZE];

        for (int i = 0; i < inputMeasurements.length; i++) {

            inputMeasurements[i] = testHelper.createMeasurement("sensorId" + i, i, (double) i);
        }

        testHelper.mockApiClientForLive(inputMeasurements);

        // When

        fetchEngine.fetch();

        // Then

        List<Measurement> measurements = testHelper.findMeasurements().stream()
                .sorted(Comparator.comparing(Measurement::getTimestamp))
                .collect(Collectors.toList());

        for (int i = 0; i < measurements.size(); i++) {

            Measurement measurement = measurements.get(i);

            assertEquals("sensorId" + i, measurement.getSensorId());
            assertEquals(i, measurement.getTimestamp().toInstant().toEpochMilli());
            assertEquals(i, measurement.getNumericValue());
        }

        assertEquals(BATCH_SIZE, measurements.size());

        assertThat(measurementReplayService.getLatestOffset()).isEmpty();

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();
    }

    @Test
    void whenMeasurementWithOffsetIsRetrieved_thenMeasurementIsInserted() throws ApiException {

        // Given

        String sensorId = "sensorId";
        long timestamp = 1L;
        double numericValue = 42d;
        String fromOffset = "fromOffset";

        Measurement measurement = testHelper.createMeasurement(sensorId, timestamp, numericValue);
        measurement.setOffset(fromOffset);
        testHelper.mockApiClientForLive(measurement);

        // When

        fetchEngine.fetch();

        // Then

        assertThat(testHelper.findMeasurements())
                .containsExactly(testHelper.createMeasurement(sensorId, timestamp, numericValue));

        assertThat(measurementReplayService.getLatestOffset()).isEmpty();

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();
    }

    @Test
    void whenMeasurementWithoutAnOffsetIsRetrieved_thenMeasurementIsNotInserted() throws ApiException {

        // Given

        testHelper.mockApiClientForLive(testHelper.createMeasurement("sensorId", 1L, 42d));

        // When

        fetchEngine.fetch();

        // Then

        assertThat(testHelper.findMeasurements()).isEmpty();

        assertThat(measurementReplayService.getLatestOffset()).isEmpty();

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();
    }

    @Test
    void givenPotentialReplay_whenMeasurementWithOffsetIsRetrieved_thenMeasurementIsInserted() throws ApiException {

        // Given

        String originalFromOffset = "originalFromOffset";

        testHelper.insertReplay(originalFromOffset, "");

        String sensorId = "sensorId";
        long timestamp = 2L;
        double numericValue = 42d;
        String newFromOffset = "newFromOffset";

        Measurement measurement = testHelper.createMeasurement(sensorId, timestamp, numericValue);
        measurement.setOffset(newFromOffset);
        testHelper.mockApiClientForLive(measurement);

        // When

        fetchEngine.fetch();

        // Then

        assertThat(testHelper.findMeasurements())
                .containsExactly(testHelper.createMeasurement(sensorId, timestamp, numericValue));

        assertThat(measurementReplayService.getLatestOffset()).hasValue(originalFromOffset);

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();
    }

    @Test
    void whenExceptionOccurs_thenRetrievingLiveMeasurementsIsRestarted() throws ApiException {

        // Given

        testHelper.mockApiClientForLive(new UnsupportedOperationException());

        fetchEngine.fetch();

        // When/then

        assertThrows(UnsupportedOperationException.class, () -> fetchEngine.fetch());

        // When

        String sensorId = "sensorId";
        long timestamp = 1L;
        double numericValue = 42d;
        String fromOffset = "fromOffset";

        Measurement measurement = testHelper.createMeasurement(sensorId, timestamp, numericValue);
        measurement.setOffset(fromOffset);
        testHelper.mockApiClientForLive(measurement);

        fetchEngine.fetch();

        // Then

        assertThat(testHelper.findMeasurements())
                .containsExactly(testHelper.createMeasurement(sensorId, timestamp, numericValue));

        assertThat(measurementReplayService.getLatestOffset()).isEmpty();

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();
    }
}
