/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import generated.dto.Measurement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class MeasurementRepository {

    private static final RowMapper<Measurement> ROW_MAPPER = (row, rowNumber) -> {
        String sensorId = row.getString("sensor_id");
        Timestamp timestamp = row.getTimestamp("timestamp");
        Object numericValue = row.getObject("numeric_value");
        String stringValue = row.getString("string_value");

        Measurement measurement = new Measurement();
        measurement.setSensorId(sensorId);
        measurement.setTimestamp(timestamp.toInstant().atOffset(ZoneOffset.UTC));

        if (numericValue != null) {
            measurement.setNumericValue((double) numericValue);
        } else {
            measurement.setStringValue(stringValue);
        }

        return measurement;
    };

    private final JdbcTemplate jdbcTemplate;

    MeasurementRepository(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    public List<Measurement> findAll() {

        return jdbcTemplate.query(
                "SELECT sensor_id, timestamp, numeric_value, string_value FROM measurement", ROW_MAPPER);
    }

    public void batchInsertOrUpdate(List<Measurement> measurements) {

        jdbcTemplate.batchUpdate(
                "MERGE INTO measurement (sensor_id, timestamp, numeric_value, string_value) VALUES (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {

                        Measurement measurement = measurements.get(index);

                        preparedStatement.setString(1, measurement.getSensorId());

                        preparedStatement.setTimestamp(
                                2, Timestamp.from(measurement.getTimestamp().toInstant()));

                        Double numericValue = measurement.getNumericValue();

                        if (numericValue != null) {
                            preparedStatement.setDouble(3, numericValue);
                        } else {
                            preparedStatement.setNull(3, Types.DOUBLE);
                        }

                        String stringValue = measurement.getStringValue();

                        if (stringValue == null) {
                            preparedStatement.setNull(4, Types.VARCHAR);
                        } else {
                            preparedStatement.setString(4, stringValue);
                        }
                    }

                    @Override
                    public int getBatchSize() {

                        return measurements.size();
                    }
                });
    }
}
