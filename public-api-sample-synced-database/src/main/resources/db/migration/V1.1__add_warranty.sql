-- Copyright © 2025 Schneider Electric. All Rights Reserved.
ALTER TABLE inventory_object
    ADD COLUMN warranty_expiration DATE;

ALTER TABLE inventory_object
    ADD COLUMN service_due_date DATE;

ALTER TABLE inventory_object
    ADD COLUMN last_service_date DATE;

ALTER TABLE inventory_object
    ADD COLUMN device_note VARCHAR;
