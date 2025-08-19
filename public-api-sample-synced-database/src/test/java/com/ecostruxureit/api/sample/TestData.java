/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import generated.dto.Alarm;
import generated.dto.Alarm.SeverityEnum;
import generated.dto.Device;
import generated.dto.Location;
import generated.dto.Organization;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Used to create test data used in several tests.
 */
@Service
@Transactional(propagation = Propagation.REQUIRED)
public class TestData {

    private final AlarmRepository alarmRepository;
    private final InventoryObjectRepository inventoryObjectRepository;
    private final JdbcTemplate jdbcTemplate;

    public TestData(
            AlarmRepository alarmRepository,
            InventoryObjectRepository inventoryObjectRepository,
            JdbcTemplate jdbcTemplate) {

        this.alarmRepository = Objects.requireNonNull(alarmRepository);
        this.inventoryObjectRepository = Objects.requireNonNull(inventoryObjectRepository);
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    /**
     * Inserts data into the database building the situation depicted on the illustration below.
     *
     * <pre>
     *
     *                                  organization_a
     *                                        |
     *                          +-------------+-------------+
     *                          |                           |
     *                     location_b                       |
     *                          |                           |
     *              +-----------+-----------+               |
     *              |                       |               |
     *          location_c                  |               |
     *              |                       |               |
     *           device_d                device_e        device_f
     *              |
     *      +-------+-------+
     *      |               |
     *   device_g        device_h
     *                      |
     *                   device_i
     *
     * </pre>
     * <p>
     * Where:
     * <ul>
     *     <li>device-d has alarm_d1
     *     <li>device-h has alarm_h1
     *     <li>device-i has alarm_i1 and alarm_i2
     * </ul>
     **/
    public void insertTestDataIntoDatabase() {

        Organization organization_a = new Organization();
        organization_a.setId("organization_a");
        organization_a.setLabel("Organization a");
        inventoryObjectRepository.insertOrUpdate(organization_a);

        Location location_b = new Location();
        location_b.setId("location_b");
        location_b.setType("BUILDING");
        location_b.setLabel("Location b");
        inventoryObjectRepository.insertOrUpdate(location_b);

        Location location_c = new Location();
        location_c.setId("location_c");
        location_c.setType("RACK");
        location_c.setLabel("Location c");
        location_c.setParentId(location_b.getId());
        inventoryObjectRepository.insertOrUpdate(location_c);

        Device device_d = new Device();
        device_d.setId("device_d");
        device_d.setType("UPS");
        device_d.setLabel("Device d");
        device_d.setLocationId(location_c.getId());
        device_d.setDeviceNote("Device note 1");
        device_d.setWarrantyExpirationDate(LocalDate.parse("2025-03-31"));
        device_d.setServiceDueDate(LocalDate.parse("2025-03-31"));
        device_d.setLastServiceDate(LocalDate.parse("2025-03-31"));
        device_d.deviceNote("Device note D");
        inventoryObjectRepository.insertOrUpdate(device_d);

        Device device_e = new Device();
        device_e.setId("device_e");
        device_e.setType("RPDU");
        device_e.setLabel("Device e");
        device_e.setLocationId(location_b.getId());
        device_e.setWarrantyExpirationDate(LocalDate.parse("2025-03-31"));
        inventoryObjectRepository.insertOrUpdate(device_e);

        Device device_f = new Device();
        device_f.setId("device_f");
        device_f.setType("RPDU");
        device_f.setLabel("Device f");
        device_f.setServiceDueDate(LocalDate.parse("2025-03-31"));
        inventoryObjectRepository.insertOrUpdate(device_f);

        Device device_g = new Device();
        device_g.setId("device_g");
        device_g.setParentId(device_d.getId());
        device_g.setType("BATTERY_FRAME");
        device_g.setLabel("Device g");
        device_g.setLocationId(location_c.getId());
        device_g.setLastServiceDate(LocalDate.parse("2025-03-31"));
        inventoryObjectRepository.insertOrUpdate(device_g);

        Device device_h = new Device();
        device_h.setId("device_h");
        device_h.setParentId(device_d.getId());
        device_h.setType("PDU");
        device_h.setLabel("Device h");
        device_h.setLocationId(location_c.getId());
        device_d.setDeviceNote("Device note 1");
        inventoryObjectRepository.insertOrUpdate(device_h);

        Device device_i = new Device();
        device_i.setId("device_i");
        device_i.setParentId(device_h.getId());
        device_i.setType("OUTLET_GROUP");
        device_i.setLabel("Device i");
        device_i.setLocationId(location_c.getId());
        inventoryObjectRepository.insertOrUpdate(device_i);

        Alarm alarm_d1 = new Alarm();
        alarm_d1.deviceId(device_d.getId());
        alarm_d1.setId("alarm_d1");
        alarm_d1.setLabel("Alarm d1");
        alarm_d1.setMessage("d1-message");
        alarm_d1.setSeverity(SeverityEnum.WARNING);
        alarm_d1.setActivatedTime(OffsetDateTime.of(2019, 8, 20, 11, 27, 0, 0, ZoneOffset.UTC));
        alarm_d1.setAlarmReactivationCount(10);
        alarmRepository.insertOrUpdate(alarm_d1);

        Alarm alarm_h1 = new Alarm();
        alarm_h1.deviceId(device_h.getId());
        alarm_h1.setId("alarm_h1");
        alarm_h1.setLabel("Alarm h1");
        alarm_h1.setMessage("h1-message");
        alarm_h1.setSeverity(SeverityEnum.ERROR);
        alarm_h1.setActivatedTime(OffsetDateTime.of(2019, 7, 10, 8, 54, 0, 0, ZoneOffset.UTC));
        alarm_h1.setClearedTime(OffsetDateTime.of(2019, 7, 10, 9, 12, 0, 0, ZoneOffset.UTC));
        alarmRepository.insertOrUpdate(alarm_h1);

        Alarm alarm_i1 = new Alarm();
        alarm_i1.deviceId(device_i.getId());
        alarm_i1.setId("alarm_i1");
        alarm_i1.setLabel("Alarm i1");
        alarm_i1.setMessage("i1-message");
        alarm_i1.setSeverity(SeverityEnum.ERROR);
        alarm_i1.setActivatedTime(OffsetDateTime.of(2019, 7, 19, 20, 3, 0, 0, ZoneOffset.UTC));
        alarmRepository.insertOrUpdate(alarm_i1);

        Alarm alarm_i2 = new Alarm();
        alarm_i2.deviceId(device_i.getId());
        alarm_i2.setId("alarm_i2");
        alarm_i2.setLabel("Alarm i2");
        alarm_i2.setMessage("i2-message");
        alarm_i2.setSeverity(SeverityEnum.WARNING);
        alarm_i2.setActivatedTime(OffsetDateTime.of(2019, 8, 3, 2, 23, 0, 0, ZoneOffset.UTC));
        alarmRepository.insertOrUpdate(alarm_i2);
    }

    /**
     * Empties inventory_object and alarm, and resets the last_api_poll.
     */
    public void resetDatabase() {

        jdbcTemplate.execute("TRUNCATE TABLE alarm");

        jdbcTemplate.execute("TRUNCATE TABLE inventory_object");

        jdbcTemplate.execute("UPDATE last_api_poll "
                + "SET current_offset = 0, last_changes_poll_time = 0, last_full_read_time = 0 "
                + "WHERE resource_name IN ('alarm', 'inventory_object')");
    }
}
