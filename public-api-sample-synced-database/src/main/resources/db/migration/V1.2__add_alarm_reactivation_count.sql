-- Copyright © 2025 Schneider Electric. All Rights Reserved.
ALTER TABLE alarm
    ADD COLUMN alarm_reactivation_count INTEGER DEFAULT 0;