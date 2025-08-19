/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import java.util.List;
import java.util.Objects;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class ReplayRepository {

    private static final RowMapper<Replay> ROW_MAPPER = (row, rowNumber) -> {
        String fromOffset = row.getString("from_offset");
        String toOffset = row.getString("to_offset");

        return new Replay(fromOffset, toOffset);
    };

    private final JdbcTemplate jdbcTemplate;

    ReplayRepository(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    public Replay findWhereToOffsetIsEmpty() {

        Replay replay = DataAccessUtils.singleResult(
                jdbcTemplate.query("SELECT from_offset, to_offset FROM replay WHERE to_offset = ''", ROW_MAPPER));

        return replay;
    }

    public List<Replay> findWhereToOffsetIsNotEmpty() {

        List<Replay> replays =
                jdbcTemplate.query("SELECT from_offset, to_offset FROM replay WHERE to_offset <> ''", ROW_MAPPER);

        return replays;
    }

    public void insert(Replay replay) {

        int updatedRows = jdbcTemplate.update(
                "INSERT INTO replay (from_offset, to_offset) VALUES (?, ?)",
                replay.getFromOffset(),
                replay.getToOffset());

        if (updatedRows != 1) {
            throw new IllegalStateException("Expected 1 updated row, got " + updatedRows + "");
        }
    }

    public void delete(Replay replay) {

        int updatedRows = jdbcTemplate.update(
                "DELETE FROM replay WHERE from_offset = ? AND to_offset = ?",
                replay.getFromOffset(),
                replay.getToOffset());

        if (updatedRows != 1) {
            throw new IllegalStateException("Expected 1 updated row, got " + updatedRows + "");
        }
    }
}
