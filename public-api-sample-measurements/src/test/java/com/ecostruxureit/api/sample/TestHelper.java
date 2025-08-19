/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import com.ecostruxureit.api.sample.client.ApiClient;
import com.ecostruxureit.api.sample.client.ApiException;
import generated.dto.Measurement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@ActiveProfiles(Profiles.TEST)
public class TestHelper {

    @MockBean
    private ApiClient apiClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private ReplayRepository replayRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    void truncateTables() {

        jdbcTemplate.update("TRUNCATE TABLE measurement");
        jdbcTemplate.update("TRUNCATE TABLE replay");
    }

    void mockApiClientForLive(Measurement... measurements) throws ApiException {

        doAnswer((invocation) -> {
                    Consumer<Measurement> consumer = invocation.getArgument(0);

                    for (Measurement measurement : measurements) {

                        consumer.accept(measurement);
                    }

                    return null;
                })
                .when(apiClient)
                .retrieveLiveMeasurements(any());
    }

    void mockApiClientForLive(Throwable throwable) throws ApiException {

        doThrow(throwable).when(apiClient).retrieveLiveMeasurements(any());
    }

    void mockApiClientForReplay(String fromOffset, String toOffset, Measurement... measurements) throws ApiException {

        doAnswer((invocation) -> {
                    Consumer<Measurement> consumer = invocation.getArgument(2);

                    for (Measurement measurement : measurements) {

                        consumer.accept(measurement);
                    }

                    return null;
                })
                .when(apiClient)
                .replayMeasurements(eq(fromOffset), eq(toOffset), any());
    }

    void mockApiClientForReplay(String fromOffset, String toOffset, Throwable throwable) throws ApiException {

        doThrow(throwable).when(apiClient).replayMeasurements(eq(fromOffset), eq(toOffset), any());
    }

    Measurement createMeasurement(String sensorId, long timestamp, Object value) {

        Measurement measurement = new Measurement();

        measurement.setSensorId(sensorId);
        measurement.setTimestamp(convertLongToOffsetDateTime(timestamp));

        if (value instanceof Double doubleValue) {
            measurement.setNumericValue(doubleValue);
        } else if (value instanceof String stringValue) {
            measurement.setStringValue(stringValue);
        } else {
            throw new IllegalArgumentException();
        }

        return measurement;
    }

    List<Measurement> findMeasurements() {

        return runInTransaction(() -> measurementRepository.findAll());
    }

    void insertReplay(String fromOffset, String toOffset) {

        runInTransaction(() -> {
            replayRepository.insert(new Replay(fromOffset, toOffset));
            return null;
        });
    }

    private static OffsetDateTime convertLongToOffsetDateTime(long timestamp) {

        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
    }

    private <T> T runInTransaction(Callable<T> callable) {

        return transactionTemplate.execute(status -> {
            try {
                return callable.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
