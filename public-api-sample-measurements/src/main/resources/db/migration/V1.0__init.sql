-- Copyright © 2025 Schneider Electric. All Rights Reserved.
-- This table stores the measurements received from the live measurements API (and possibly the replay measurements API as well). Also see
-- the MeasurementRepository class.
CREATE TABLE measurement
(
    sensor_id    VARCHAR   NOT NULL,
    timestamp    TIMESTAMP NOT NULL,
    numeric_value DOUBLE,
    string_value VARCHAR,
    PRIMARY KEY (sensor_id, timestamp)
);

-- This table stores pending replays to be performed using the replay measurements API. It also stores a special row (with to_offset being
-- the empty string) that is used to keep track of the latest offset received from the live measurements API. Also see the ReplayRepository
-- class.
CREATE TABLE replay
(
    from_offset VARCHAR NOT NULL,
    to_offset   VARCHAR NOT NULL,
    PRIMARY KEY (from_offset, to_offset)
);
