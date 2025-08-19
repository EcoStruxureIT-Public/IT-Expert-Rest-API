/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import generated.dto.Alarm;
import generated.dto.Alarm.SeverityEnum;
import generated.dto.Device;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the methods in {@link AlarmRepository}.
 * <p>
 * Using the {@link Transactional} annotation on a test class means that test methods will each be run in a separate transaction, which is
 * rolled back, when the test finishes - which means test methods won't "pollute" the database with data that may make other tests fail.
 */
@SpringBootTest
@ActiveProfiles(Profiles.TEST)
@Transactional
class AlarmRepositoryTest {

    @Autowired
    private AlarmRepository alarmRepository;

    @Test
    void insertOrUpdateAlarm() {

        String alarmId = "e87e428f-0e52-4b8a-877f-c9f465fa27e3";
        String deviceId = "6db8ca39-65c8-4634-ba73-22f11e89f4ab";
        OffsetDateTime activatedTime = OffsetDateTime.parse("2019-08-29T10:15:30.00Z");
        OffsetDateTime clearedTime = OffsetDateTime.parse("2019-08-30T08:23:34.00Z");
        int alarmReactivationCount = 10;

        // Insert first version of an alarm into the database
        Alarm alarmV1 = new Alarm();
        alarmV1.setId(alarmId);
        alarmV1.setDeviceId(deviceId);
        alarmV1.setLabel("label-v1");
        alarmV1.setMessage("message-v1");
        alarmV1.setSeverity(SeverityEnum.INFO);
        alarmV1.setActivatedTime(activatedTime);
        alarmV1.setClearedTime(null); // not yet cleared - which means this alarm is still active
        alarmV1.alarmReactivationCount(alarmReactivationCount);
        alarmRepository.insertOrUpdate(alarmV1);

        // Verify that if we load the inserted alarm from the database again, then it looks as expected
        Alarm alarmV1FromRepository = alarmRepository.findById(alarmId);
        assertEquals(alarmId, alarmV1FromRepository.getId());
        assertEquals(deviceId, alarmV1FromRepository.getDeviceId());
        assertEquals("label-v1", alarmV1FromRepository.getLabel());
        assertEquals("message-v1", alarmV1FromRepository.getMessage());
        assertEquals(SeverityEnum.INFO, alarmV1FromRepository.getSeverity());
        assertEquals(activatedTime, alarmV1FromRepository.getActivatedTime());
        assertEquals(alarmReactivationCount, alarmV1FromRepository.getAlarmReactivationCount());
        assertNull(alarmV1FromRepository.getClearedTime());

        // Now create new version of the alarm
        Alarm alarmV2 = new Alarm();
        alarmV2.setId(
                alarmId); // Has same ID as the original alarm, so this is just a changed version of the original alarm
        alarmV2.setDeviceId(deviceId);
        alarmV2.setLabel("label-v2");
        alarmV2.setMessage("message-v2");
        alarmV2.setSeverity(SeverityEnum.WARNING);
        alarmV2.setActivatedTime(activatedTime);
        alarmV2.setClearedTime(clearedTime);
        alarmRepository.insertOrUpdate(alarmV2); // should end up replacing alarmV1 as it has the same ID

        // Verify that if now load the alarm from the database, then it reflects the 2nd version of the alarm
        Alarm alarmV2FromRepository = alarmRepository.findById(alarmId);
        assertEquals(alarmId, alarmV2FromRepository.getId());
        assertEquals(deviceId, alarmV2FromRepository.getDeviceId());
        assertEquals("label-v2", alarmV2FromRepository.getLabel());
        assertEquals("message-v2", alarmV2FromRepository.getMessage());
        assertEquals(SeverityEnum.WARNING, alarmV2FromRepository.getSeverity());
        assertEquals(activatedTime, alarmV2FromRepository.getActivatedTime());
        assertEquals(clearedTime, alarmV2FromRepository.getClearedTime());
    }

    @Test
    void findById() {

        Alarm alarm1 = new Alarm();
        alarm1.setId("5bb1a628-a92c-4000-aee2-b877e68b71a0");
        alarmRepository.insertOrUpdate(alarm1);

        Alarm alarm2 = new Alarm();
        alarm2.setId("42f429dd-617c-417e-815e-94416444fe3d");
        alarmRepository.insertOrUpdate(alarm2);

        assertNotNull(alarmRepository.findById("5bb1a628-a92c-4000-aee2-b877e68b71a0"));
        assertNotNull(alarmRepository.findById("42f429dd-617c-417e-815e-94416444fe3d"));
        assertNull(alarmRepository.findById("9e3bbd5a-8d97-4f81-b141-a4466a5f551e"));
    }

    @Test
    void findByDevice() {

        Device device1 = new Device();
        device1.setId("c51c2a0d-5308-4299-a26a-f6ba317657fb");

        Device device2 = new Device();
        device2.setId("266250ad-0557-471d-a813-6487ed9a7be8");

        Device device3 = new Device();
        device3.setId("e6f7a57b-0371-4794-914b-d64e49dfa818");

        Alarm alarm1 = new Alarm();
        alarm1.setId("5bb1a628-a92c-4000-aee2-b877e68b71a0");
        alarm1.setDeviceId(device1.getId());
        alarmRepository.insertOrUpdate(alarm1);

        Alarm alarm2 = new Alarm();
        alarm2.setId("42f429dd-617c-417e-815e-94416444fe3d");
        alarm2.setDeviceId(device2.getId());
        alarmRepository.insertOrUpdate(alarm2);

        Alarm alarm3 = new Alarm();
        alarm3.setId("0c92116d-280c-4df3-bcf0-6a42bf30df12");
        alarm3.setDeviceId(device1.getId());
        alarmRepository.insertOrUpdate(alarm3);

        assertEquals(2, alarmRepository.findByDevice(device1).size());
        assertEquals(1, alarmRepository.findByDevice(device2).size());
        assertEquals(0, alarmRepository.findByDevice(device3).size());
    }

    @Test
    void findAll() {

        Alarm alarm1 = new Alarm();
        alarm1.setId("5bb1a628-a92c-4000-aee2-b877e68b71a0");
        alarmRepository.insertOrUpdate(alarm1);

        Alarm alarm2 = new Alarm();
        alarm2.setId("42f429dd-617c-417e-815e-94416444fe3d");
        alarmRepository.insertOrUpdate(alarm2);

        List<Alarm> allAlarms = alarmRepository.findAll();

        assertEquals(2, allAlarms.size());
    }

    @Test
    void findAllIds() {

        Alarm alarm1 = new Alarm();
        alarm1.setId("3d8ef983-7d70-4d4f-9134-e4200d08374f");
        alarmRepository.insertOrUpdate(alarm1);

        Alarm alarm2 = new Alarm();
        alarm2.setId("dc78a469-d0cc-4972-abaa-d4f1f82fa468");
        alarmRepository.insertOrUpdate(alarm2);

        Set<String> allIds = alarmRepository.findAllIds();

        assertEquals(2, allIds.size());
        assertTrue(allIds.contains(alarm1.getId()));
        assertTrue(allIds.contains(alarm2.getId()));
    }

    @Test
    void deleteByIds() {

        Alarm alarm1 = new Alarm();
        alarm1.setId("fc0dc73e-19ab-4d4e-896f-cbed03e1cf4c");
        alarm1.setLabel("Alarm1");
        alarmRepository.insertOrUpdate(alarm1);

        Alarm alarm2 = new Alarm();
        alarm2.setId("ea88bf33-3bf8-4b59-80a8-1f595efc474c");
        alarm2.setLabel("Alarm2");
        alarmRepository.insertOrUpdate(alarm2);

        Alarm alarm3 = new Alarm();
        alarm3.setId("408dc88e-169d-4101-998b-1aafe94cbb0b");
        alarm3.setLabel("Alarm3");
        alarmRepository.insertOrUpdate(alarm3);

        Set<String> idsToDelete = new HashSet<>();
        idsToDelete.add(alarm1.getId());
        idsToDelete.add(alarm3.getId());
        alarmRepository.deleteByIds(idsToDelete);

        List<Alarm> allAlarms = alarmRepository.findAll();

        assertEquals(1, allAlarms.size());
        assertEquals("Alarm2", allAlarms.getFirst().getLabel());
    }

    @Test
    void deleteByIds_whenMoreThan100Ids_thenStillWorks() {

        Alarm donNotDeleteMe = new Alarm();
        donNotDeleteMe.setId("a2fd5041-4ecb-40c8-be92-d07f129bd775");
        alarmRepository.insertOrUpdate(donNotDeleteMe);

        int entriesToKeep = 1;
        int entriesToDelete = 110;

        Set<String> idsToDelete = Sets.newHashSetWithExpectedSize(entriesToDelete);

        for (int n = 0; n < entriesToDelete; n++) {
            String id = ("id-" + n);
            idsToDelete.add(id);
            Alarm alarmToDelete = new Alarm();
            alarmToDelete.setId(id);
            alarmRepository.insertOrUpdate(alarmToDelete);
        }

        assertEquals(entriesToDelete + entriesToKeep, alarmRepository.findAll().size());

        alarmRepository.deleteByIds(idsToDelete);

        assertEquals(entriesToKeep, alarmRepository.findAll().size());
    }
}
