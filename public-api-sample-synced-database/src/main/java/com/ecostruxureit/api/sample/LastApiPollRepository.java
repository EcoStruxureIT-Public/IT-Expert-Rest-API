package com.ecostruxureit.api.sample;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Handles persistence of {@link LastApiPoll} objects in the {@code last_api_poll} database table.
 * <p>
 * Also handles mapping between database {@code last_api_poll} rows and Java {@link LastApiPoll} objects.
 */
@Repository
@Transactional(propagation = Propagation.REQUIRED)
class LastApiPollRepository {

    // Used to map from a row in the last_api_poll table to an LastApiPoll object
    private static final RowMapper<LastApiPoll> ROW_MAPPER = (row, rowNumber) -> {
        LastApiPoll lastApiPoll = new LastApiPoll();
        lastApiPoll.setResourceName(row.getString("resource_name"));
        lastApiPoll.setCurrentOffset(row.getLong("current_offset"));
        lastApiPoll.setLastChangesPollTime(row.getLong("last_changes_poll_time"));
        lastApiPoll.setLastFullReadTime(row.getLong("last_full_read_time"));
        return lastApiPoll;
    };

    private final JdbcTemplate jdbcTemplate;

    LastApiPollRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    /**
     * @param lastApiPoll updates the row in the last_api_poll that represents the given resource.
     */
    void updateLastApiPoll(LastApiPoll lastApiPoll) {

        // Note that V1.0__init.sql inserts initial versions of the 2 rows, so we can always just do updates (never
        // inserts).
        int updatedRows = jdbcTemplate.update(
                "UPDATE last_api_poll "
                        + "SET current_offset = ?, last_changes_poll_time = ?, last_full_read_time = ? "
                        + "WHERE resource_name = ?",
                lastApiPoll.getCurrentOffset(),
                lastApiPoll.getLastChangesPollTime(),
                lastApiPoll.getLastFullReadTime(),
                lastApiPoll.getResourceName());

        if (updatedRows != 1) {
            throw new UnexpectedNumberOfRowsAffectedException(1, updatedRows);
        }
    }

    LastApiPoll findLastApiPollForAlarm() {
        return findLastApiPollByResourceName(LastApiPoll.RESOURCE_NAME_FOR_ALARM);
    }

    LastApiPoll findLastApiPollForInventoryObject() {
        return findLastApiPollByResourceName(LastApiPoll.RESOURCE_NAME_FOR_INVENTORY_OBJECT);
    }

    private LastApiPoll findLastApiPollByResourceName(String resourceName) {

        return jdbcTemplate.queryForObject(
                "SELECT resource_name, current_offset, last_changes_poll_time, last_full_read_time"
                        + " FROM last_api_poll WHERE resource_name = ?",
                ROW_MAPPER,
                resourceName);
    }
}
