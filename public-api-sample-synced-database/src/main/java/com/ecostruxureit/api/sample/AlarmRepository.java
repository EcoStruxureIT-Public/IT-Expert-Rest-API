/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import com.google.common.collect.Sets;
import generated.dto.Alarm;
import generated.dto.Alarm.SeverityEnum;
import generated.dto.Device;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles persistence of {@link Alarm} objects in the {@code alarm} database table.
 * <p>
 * Also handles mapping between database alarm rows and Java {@link Alarm} objects.
 */
@Repository
@Transactional(propagation = Propagation.REQUIRED)
public class AlarmRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmRepository.class);

    // Used to map from a row in the alarm table to an Alarm object
    private static final RowMapper<Alarm> ALARM_ROW_MAPPER = (row, rowNumber) -> {
        Alarm alarm = new Alarm();
        alarm.setId(row.getString("id"));
        alarm.setDeviceId(row.getString("device_id"));
        alarm.setLabel(row.getString("label"));
        alarm.setMessage(row.getString("message"));
        alarm.setSeverity(convertStringToSeverity(row.getString("severity")));
        alarm.setActivatedTime(convertTimestampToOffsetDateTime(row.getTimestamp("activated_time")));
        alarm.setClearedTime(convertTimestampToOffsetDateTime(row.getTimestamp("cleared_time")));
        alarm.setAlarmReactivationCount(row.getInt("alarm_reactivation_count"));
        return alarm;
    };

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    AlarmRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.namedParameterJdbcTemplate = Objects.requireNonNull(namedParameterJdbcTemplate);
    }

    /**
     * Inserts the Alarm into the alarm table if no row with the given ID exists - or updates the existing alarm row if one already exists.
     *
     * @param alarm the alarm to insert or update.
     * @throws UnexpectedNumberOfRowsAffectedException if the executed SQL did not result in exactly 1 row being inserted or changed.
     */
    void insertOrUpdate(Alarm alarm) throws UnexpectedNumberOfRowsAffectedException {

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("id", alarm.getId());
        parameters.addValue("device_id", alarm.getDeviceId());
        parameters.addValue("label", alarm.getLabel());
        parameters.addValue("message", alarm.getMessage());
        parameters.addValue("severity", convertSeverityToString(alarm.getSeverity()));
        parameters.addValue("activated_time", convertOffsetDateTimeToTimestamp(alarm.getActivatedTime()));
        parameters.addValue("cleared_time", convertOffsetDateTimeToTimestamp(alarm.getClearedTime()));
        parameters.addValue("alarm_reactivation_count", alarm.getAlarmReactivationCount());

        int updatedRows = namedParameterJdbcTemplate.update(
                // MERGE means insert if not exists otherwise replace - many databases have a similar statement.
                // Otherwise will have to a combination of (insert or update) or (delete and insert) depending on the
                // database.
                "MERGE INTO alarm (id, device_id, label, message, severity, activated_time, cleared_time, alarm_reactivation_count) "
                        + "KEY "
                        + "(id)"
                        + " VALUES (:id, :device_id, :label, :message, :severity, :activated_time, :cleared_time, :alarm_reactivation_count);"
                        + "\n",
                parameters);

        if (updatedRows != 1) {
            throw new UnexpectedNumberOfRowsAffectedException(1, updatedRows);
        }
    }

    /**
     * Finds the alarm with the given ID in the alarm table.
     *
     * @param id of the alarm to find.
     * @return the alarm with the given ID - or null if none were found.
     */
    Alarm findById(String id) {

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("id", id);

        try {
            return namedParameterJdbcTemplate.queryForObject(
                    "SELECT id, device_id, label, message, severity, activated_time, cleared_time, alarm_reactivation_count FROM alarm WHERE "
                            + "id = :id",
                    parameters,
                    ALARM_ROW_MAPPER);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Finds all alarms that have occurred on the given device - that is alarms where device.id = alarm.device_id.
     *
     * @param device the Device who's alarms to find.
     * @return all the alarms that has occurred on the given device.
     */
    public List<Alarm> findByDevice(Device device) {

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("device_id", device.getId());

        List<Alarm> result = namedParameterJdbcTemplate.query(
                "SELECT id, device_id, label, message, severity, activated_time, cleared_time, alarm_reactivation_count FROM alarm WHERE "
                        + "device_id = :device_id",
                parameters,
                ALARM_ROW_MAPPER);

        LOGGER.trace(
                "{}.findByDevice({}) --> {}",
                getClass().getSimpleName(),
                device.getId(),
                result.stream().map(Alarm::getId).toList());

        return result;
    }

    /**
     * Finds all the alarms in the alarm table.
     *
     * @return all alarms.
     */
    public List<Alarm> findAll() {
        return jdbcTemplate.query(
                "SELECT id, device_id, label, message, severity, activated_time, cleared_time, alarm_reactivation_count FROM alarm ORDER BY "
                        + "device_id",
                ALARM_ROW_MAPPER);
    }

    /**
     * Finds all the alarms in the alarm table, and return their IDs.
     *
     * @return the IDs of all alarms.
     */
    Set<String> findAllIds() {
        return new HashSet<>(jdbcTemplate.queryForList("SELECT id FROM alarm", String.class));
    }

    /**
     * Deletes all the alarms from the alarm table, that have one of the given IDs. Silently ignores if trying to delete a non-existing ID.
     *
     * @param ids the IDs of the alarms to delete.
     */
    void deleteByIds(Set<String> ids) {

        // The JDBC standard does not guarantee that you can use more than 100 values for an IN expression list.
        // Various databases exceed this number, but they usually have a hard limit for how many values are allowed.
        // See https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#jdbc-in-clause
        // Therefore, we delete the ids in chunks of no more than 100 per chunk.

        Set<String> max100ids = Sets.newHashSetWithExpectedSize(100);
        for (String id : ids) {
            max100ids.add(id);
            if (max100ids.size() == 100) {
                deleteByIdsMax100(max100ids);
                max100ids.clear();
            }
        }
        if (max100ids.size() > 0) {
            deleteByIdsMax100(max100ids);
        }
    }

    private void deleteByIdsMax100(Set<String> ids) {

        if (ids.size() > 100) {
            throw new IllegalArgumentException(
                    "Parameter ids must not contain more than 100 entries, but contained " + ids.size());
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", ids);

        namedParameterJdbcTemplate.update("DELETE FROM alarm WHERE id IN (:ids)", parameters);
    }

    private static String convertSeverityToString(SeverityEnum severity) {
        if (severity == null) {
            return null;
        }
        return severity.toString();
    }

    private static SeverityEnum convertStringToSeverity(String severity) {
        if (severity == null) {
            return null;
        }
        return SeverityEnum.fromValue(severity);
    }

    private static Timestamp convertOffsetDateTimeToTimestamp(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return Timestamp.from(offsetDateTime.toInstant());
    }

    private static OffsetDateTime convertTimestampToOffsetDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
