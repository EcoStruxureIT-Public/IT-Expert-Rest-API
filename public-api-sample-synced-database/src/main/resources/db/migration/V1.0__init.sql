-- Copyright (c) 2025 Schneider Electric. All Rights Reserved.
-- Used to "remember" offset and the last time each type of polls were made. Also see the LastApiPoll class.
CREATE TABLE last_api_poll
(
    resource_name          VARCHAR PRIMARY KEY,
    current_offset         BIGINT NOT NULL,
    last_changes_poll_time BIGINT NOT NULL,
    last_full_read_time    BIGINT NOT NULL
);

INSERT INTO last_api_poll (resource_name, current_offset, last_changes_poll_time, last_full_read_time)
VALUES ('alarm', 0, 0, 0);

INSERT INTO last_api_poll (resource_name, current_offset, last_changes_poll_time, last_full_read_time)
VALUES ('inventory_object', 0, 0, 0);

-- This table stores is used to store all inventory object subtypes - each row is EITHER a device, a location, or an organization.
-- The 'discriminator' column tells which type of object contains (e.g. 'Device' if it contains a device).
-- Note that not all columns are used by all types of objects stored - see comment on individual columns below.
-- The data you retrieve from the API is eventually consistent - therefore you should avoid using foreign key constraints.
-- Note: The sample doesn't have columns to store all the properties on devices - e.g. modelName, serialNumber, and several more.
CREATE TABLE inventory_object
(
    id                 VARCHAR PRIMARY KEY,
    discriminator      VARCHAR, -- Tells whether this row contains an Organization, a Location, or a Device
    label              VARCHAR,
    type               VARCHAR, -- Used for location type and device type
    device_parent_id   VARCHAR, -- Only used by Device (sub devices referencing their parent device)
    location_parent_id VARCHAR, -- Only used by Device and Location (device referring to its location, and location to its parent location)
    gateway_ids        VARCHAR, -- A comma separated list of IDs
    address            VARCHAR  -- Only used by Organization and Location
);

-- This table stores all alarms - both active and cleared (cleared alarms don't have a value in cleared_time yet).
-- The data you retrieve from the API is eventually consistent - therefore you should avoid using foreign key constraints.
CREATE TABLE alarm
(
    id             VARCHAR PRIMARY KEY,
    device_id      VARCHAR, -- References the ID of the device on which the alarm occurred.
    label          VARCHAR,
    message        VARCHAR,
    severity       VARCHAR,
    activated_time TIMESTAMP,
    cleared_time   TIMESTAMP
);
