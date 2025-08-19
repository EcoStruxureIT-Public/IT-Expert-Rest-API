/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecostruxureit.api.sample.client.ApiException;
import generated.dto.Measurement;
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
@TestPropertySource(properties = "replayEnabled=true")
class FetchEngineWithReplayEnabledTest {

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
    void whenMeasurementWithOffsetIsRetrieved_thenMeasurementAndPotentialReplayAreInserted() throws ApiException {

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

        assertThat(measurementReplayService.getLatestOffset()).hasValue(fromOffset);

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();
    }

    @Test
    void
            givenPotentialReplay_whenMeasurementWithOffsetIsRetrieved_thenMeasurementIsInsertedAndPendingReplayIsInsertedAndPotentialReplayIsUpdated()
                    throws ApiException {

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

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();

        assertThat(measurementReplayService.getLatestOffset()).hasValue(newFromOffset);
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

        assertThat(measurementReplayService.getLatestOffset()).hasValue(fromOffset);

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();
    }

    @Test
    void givenPendingReplay_whenMeasurementIsReplayed_thenMeasurementIsInsertedAndPendingReplayIsDeleted()
            throws ApiException {

        // Given

        String fromOffset = "fromOffset";
        String toOffset = "toOffset";

        testHelper.insertReplay(fromOffset, toOffset);

        Measurement measurement = testHelper.createMeasurement("sensorId", 1L, 42d);

        testHelper.mockApiClientForReplay(fromOffset, toOffset, measurement);

        // When

        fetchEngine.fetch();

        // Then

        assertThat(testHelper.findMeasurements()).containsExactly(measurement);

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();
    }

    @Test
    void whenExceptionOccurs_thenReplayingMeasurementsIsRestarted() throws ApiException {

        // Given

        String fromOffset = "fromOffset";
        String toOffset = "toOffset";

        testHelper.insertReplay(fromOffset, toOffset);

        testHelper.mockApiClientForReplay(fromOffset, toOffset, new UnsupportedOperationException());

        fetchEngine.fetch();

        // When/then

        assertThrows(UnsupportedOperationException.class, () -> fetchEngine.fetch());

        // When

        String sensorId = "sensorId";
        long timestamp = 1L;
        double numericValue = 42d;

        testHelper.mockApiClientForReplay(
                fromOffset, toOffset, testHelper.createMeasurement(sensorId, timestamp, numericValue));

        fetchEngine.fetch();

        // Then

        assertThat(testHelper.findMeasurements())
                .containsExactly(testHelper.createMeasurement(sensorId, timestamp, numericValue));

        assertThat(measurementReplayService.findPendingReplays()).isEmpty();
    }
}
