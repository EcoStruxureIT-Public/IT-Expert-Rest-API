/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import generated.dto.Alarm;
import generated.dto.AlarmChangesResponse;
import generated.dto.AlarmsResponse;
import generated.dto.Device;
import generated.dto.InventoryChangesResponse;
import generated.dto.InventoryObject;
import generated.dto.InventoryObjectCreateOrUpdate;
import generated.dto.InventoryObjectDelete;
import generated.dto.InventoryResponse;
import generated.dto.Location;
import generated.dto.Organization;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the methods in {@link FetchEngine}.
 * <p>
 * Using the {@link Transactional} annotation on a test class means that test methods will each be run in a separate transaction, which is
 * rolled back, when the test finishes - which means test methods won't "pollute" the database with data that may make other tests fail.
 */
@SpringBootTest
@ActiveProfiles(Profiles.TEST)
@Transactional
class FetchEngineTest {

    @MockBean
    private RestClient restClient;

    @MockBean
    private Clock clock;

    @Autowired
    private FetchEngine fetchEngine;

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private InventoryObjectRepository inventoryObjectRepository;

    @Autowired
    private LastApiPollRepository lastApiPollRepository;

    private static final long NOW = 1568200692543L;
    private static final long LESS_THAN_ONE_DAY_BEFORE_NOW =
            NOW - Duration.ofDays(1).toMillis() + 1;
    private static final long MORE_THAN_ONE_DAY_BEFORE_NOW =
            NOW - Duration.ofDays(1).toMillis() - 1;
    private static final long MORE_THAN_ONE_WEEK_BEFORE_NOW =
            NOW - Duration.ofDays(7).toMillis() - 1;
    private static final long JUST_AFTER_NOW = NOW + 1;

    @Test
    void shouldDoFullRead() {

        LastApiPoll lastApiPoll = new LastApiPoll();

        lastApiPoll.setLastFullReadTime(MORE_THAN_ONE_DAY_BEFORE_NOW);
        lastApiPoll.setLastChangesPollTime(LESS_THAN_ONE_DAY_BEFORE_NOW);
        assertFalse(
                FetchEngine.shouldDoFullRead(NOW, lastApiPoll),
                "Last read (changes) was less than 1 day ago -> read changes");

        lastApiPoll.setLastFullReadTime(LESS_THAN_ONE_DAY_BEFORE_NOW);
        lastApiPoll.setLastChangesPollTime(MORE_THAN_ONE_DAY_BEFORE_NOW);
        assertFalse(
                FetchEngine.shouldDoFullRead(NOW, lastApiPoll),
                "Last read (full) was less than 1 day ago -> read changes");

        lastApiPoll.setLastFullReadTime(LESS_THAN_ONE_DAY_BEFORE_NOW);
        lastApiPoll.setLastChangesPollTime(LESS_THAN_ONE_DAY_BEFORE_NOW);
        assertFalse(
                FetchEngine.shouldDoFullRead(NOW, lastApiPoll),
                "Last reads (both) were less than 1 day ago -> read changes");

        lastApiPoll.setLastFullReadTime(MORE_THAN_ONE_DAY_BEFORE_NOW);
        lastApiPoll.setLastChangesPollTime(MORE_THAN_ONE_DAY_BEFORE_NOW);
        assertTrue(
                FetchEngine.shouldDoFullRead(NOW, lastApiPoll),
                "Last reads (both) were more than 1 days ago -> read full");

        lastApiPoll.setLastFullReadTime(MORE_THAN_ONE_WEEK_BEFORE_NOW);
        lastApiPoll.setLastChangesPollTime(LESS_THAN_ONE_DAY_BEFORE_NOW);
        assertTrue(
                FetchEngine.shouldDoFullRead(NOW, lastApiPoll), "Last full read was more than 7 days ago -> read full");
    }

    @Test
    void fetchInventoryObjects_givenStartupStateInDatabase_whenFullRead_thenInsertsAll() {

        // When

        InventoryResponse mockResponse = new InventoryResponse();
        mockResponse.addInventoryObjectsItem(createOrganization("1", "org-1"));
        mockResponse.addInventoryObjectsItem(createLocation("2", "loc-2"));
        mockResponse.addInventoryObjectsItem(createDevice("3", "dev-3"));
        mockResponse.setOffset(24L);
        Mockito.when(restClient.listInventory()).thenReturn(mockResponse);

        Mockito.when(clock.currentTimeMillis()).thenReturn(NOW);

        fetchEngine.fetchInventoryObjects();

        // Then

        LastApiPoll lastApiPollForInventoryObject = lastApiPollRepository.findLastApiPollForInventoryObject();
        assertEquals(NOW, lastApiPollForInventoryObject.getLastFullReadTime());
        assertEquals(0L, lastApiPollForInventoryObject.getLastChangesPollTime());
        assertEquals(24L, lastApiPollForInventoryObject.getCurrentOffset());

        assertEquals(3, inventoryObjectRepository.findAll().size());
    }

    @Test
    void
            fetchInventoryObjects_givenSomeInventoryObjectsAlreadyInDatabase_whenFullRead_thenDeletesRemovedAndUpdatesRest() {

        // Given

        InventoryObject existingInventoryObject1 = createOrganization("1", "org-1");
        InventoryObject existingInventoryObject2 = createLocation("2", "loc-2");
        InventoryObject existingInventoryObject3 = createDevice("3", "dev-3");

        inventoryObjectRepository.insertOrUpdate(existingInventoryObject1);
        inventoryObjectRepository.insertOrUpdate(existingInventoryObject2);
        inventoryObjectRepository.insertOrUpdate(existingInventoryObject3);

        LastApiPoll lastApiPoll = lastApiPollRepository.findLastApiPollForInventoryObject();
        lastApiPoll.setLastChangesPollTime(LESS_THAN_ONE_DAY_BEFORE_NOW);
        lastApiPoll.setLastFullReadTime(MORE_THAN_ONE_WEEK_BEFORE_NOW); // Forces full read
        lastApiPoll.setCurrentOffset(12L);

        lastApiPollRepository.updateLastApiPoll(lastApiPoll);

        // When

        InventoryObject unmodifiedInventoryObject1 = createOrganization("1", "org-1"); // <= note same label
        // We simulate that inventoryObject2 has been deleted
        InventoryObject modifiedInventoryObject3 = createDevice("3", "dev-3-modified"); // <= note modified label

        InventoryResponse mockResponse = new InventoryResponse();
        mockResponse.addInventoryObjectsItem(unmodifiedInventoryObject1);
        mockResponse.addInventoryObjectsItem(modifiedInventoryObject3);
        mockResponse.setOffset(24L);
        Mockito.when(restClient.listInventory()).thenReturn(mockResponse);

        Mockito.when(clock.currentTimeMillis()).thenReturn(NOW);

        fetchEngine.fetchInventoryObjects();

        // Then

        LastApiPoll lastApiPollForInventoryObject = lastApiPollRepository.findLastApiPollForInventoryObject();
        assertEquals(NOW, lastApiPollForInventoryObject.getLastFullReadTime());
        assertEquals(LESS_THAN_ONE_DAY_BEFORE_NOW, lastApiPollForInventoryObject.getLastChangesPollTime());
        assertEquals(24L, lastApiPollForInventoryObject.getCurrentOffset());

        assertEquals(2, inventoryObjectRepository.findAll().size());
        assertEquals("org-1", inventoryObjectRepository.findById("1").getLabel());
        assertEquals("dev-3-modified", inventoryObjectRepository.findById("3").getLabel());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void
            fetchInventoryObjects_givenSomeInventoryObjectsAlreadyInDatabase_whenChangesRead_thenDeletesRemovedAndUpdatesRest(
                    boolean testDuplicateReads) {

        // Given

        InventoryObject existingInventoryObject1 = createOrganization("1", "org-1");
        InventoryObject existingInventoryObject2 = createLocation("2", "loc-2");
        InventoryObject existingInventoryObject3 = createDevice("3", "dev-3");

        inventoryObjectRepository.insertOrUpdate(existingInventoryObject1);
        inventoryObjectRepository.insertOrUpdate(existingInventoryObject2);
        inventoryObjectRepository.insertOrUpdate(existingInventoryObject3);

        LastApiPoll lastApiPoll = lastApiPollRepository.findLastApiPollForInventoryObject();
        lastApiPoll.setLastChangesPollTime(LESS_THAN_ONE_DAY_BEFORE_NOW);
        lastApiPoll.setLastFullReadTime(LESS_THAN_ONE_DAY_BEFORE_NOW); // Change read is enough
        lastApiPoll.setCurrentOffset(12L);

        lastApiPollRepository.updateLastApiPoll(lastApiPoll);

        // When

        // We simulate that inventoryObject1 has not changed
        // We simulate that inventoryObject2 has been deleted
        InventoryObject modifiedInventoryObject3 = createDevice("3", "dev-3-modified"); // <= note modified label

        InventoryChangesResponse mockResponse = new InventoryChangesResponse();
        InventoryObjectCreateOrUpdate change1 = new InventoryObjectCreateOrUpdate();
        change1.setInventoryObject(modifiedInventoryObject3);
        mockResponse.addInventoryObjectChangesItem(change1);
        InventoryObjectDelete change2 = new InventoryObjectDelete();
        change2.setInventoryObjectId("2");
        mockResponse.addInventoryObjectChangesItem(change2);
        mockResponse.setOffset(24L);
        Mockito.when(restClient.getInventoryChangesAfterOffset(12L)).thenReturn(mockResponse);

        Mockito.when(clock.currentTimeMillis()).thenReturn(NOW);

        fetchEngine.fetchInventoryObjects();

        if (testDuplicateReads) {
            // In some cases the API may return the same changes more than once
            // By calling the API twice while getting same result back both times, we verify that the engine can handle
            // duplicate changes
            mockResponse.setOffset(28L);
            Mockito.when(restClient.getInventoryChangesAfterOffset(24L))
                    .thenReturn(mockResponse); // Offset was updated by previous read
            Mockito.when(clock.currentTimeMillis()).thenReturn(JUST_AFTER_NOW); // Time has passed since now
            fetchEngine.fetchInventoryObjects();
        }

        // Then

        LastApiPoll lastApiPollForInventoryObject = lastApiPollRepository.findLastApiPollForInventoryObject();
        assertEquals(LESS_THAN_ONE_DAY_BEFORE_NOW, lastApiPollForInventoryObject.getLastFullReadTime());
        assertEquals(
                (testDuplicateReads ? JUST_AFTER_NOW : NOW), lastApiPollForInventoryObject.getLastChangesPollTime());
        assertEquals((testDuplicateReads ? 28L : 24L), lastApiPollForInventoryObject.getCurrentOffset());

        assertEquals(2, inventoryObjectRepository.findAll().size());
        assertEquals("org-1", inventoryObjectRepository.findById("1").getLabel());
        assertEquals("dev-3-modified", inventoryObjectRepository.findById("3").getLabel());
    }

    @Test
    void fetchAlarms_givenStartupStateInDatabase_whenFullRead_thenInsertsAll() {

        // When

        AlarmsResponse mockResponse = new AlarmsResponse();
        mockResponse.addAlarmsItem(createAlarm("1", "alarm-1"));
        mockResponse.addAlarmsItem(createAlarm("2", "alarm-2"));
        mockResponse.addAlarmsItem(createAlarm("3", "alarm-3"));
        mockResponse.setOffset(24L);
        Mockito.when(restClient.listAlarms()).thenReturn(mockResponse);

        Mockito.when(clock.currentTimeMillis()).thenReturn(NOW);

        fetchEngine.fetchAlarms();

        // Then

        LastApiPoll lastApiPollForAlarm = lastApiPollRepository.findLastApiPollForAlarm();
        assertEquals(NOW, lastApiPollForAlarm.getLastFullReadTime());
        assertEquals(0L, lastApiPollForAlarm.getLastChangesPollTime());
        assertEquals(24L, lastApiPollForAlarm.getCurrentOffset());

        assertEquals(3, alarmRepository.findAll().size());
    }

    @Test
    void fetchAlarms_givenSomeAlarmsAlreadyInDatabase_whenFullRead_thenDeletesRemovedAndUpdatesRest() {

        // Given

        Alarm existingAlarm1 = createAlarm("1", "alarm-1");
        Alarm existingAlarm2 = createAlarm("2", "alarm-2");
        Alarm existingAlarm3 = createAlarm("3", "alarm-3");

        alarmRepository.insertOrUpdate(existingAlarm1);
        alarmRepository.insertOrUpdate(existingAlarm2);
        alarmRepository.insertOrUpdate(existingAlarm3);

        LastApiPoll lastApiPoll = lastApiPollRepository.findLastApiPollForAlarm();
        lastApiPoll.setLastChangesPollTime(LESS_THAN_ONE_DAY_BEFORE_NOW);
        lastApiPoll.setLastFullReadTime(MORE_THAN_ONE_WEEK_BEFORE_NOW); // Forces full read
        lastApiPoll.setCurrentOffset(12L);

        lastApiPollRepository.updateLastApiPoll(lastApiPoll);

        // When

        Alarm unmodifiedAlarm1 = createAlarm("1", "alarm-1"); // <= note same label
        // We simulate that alarm2 has been deleted
        Alarm modifiedAlarm3 = createAlarm("3", "alarm-3-modified"); // <= note modified label

        AlarmsResponse mockResponse = new AlarmsResponse();
        mockResponse.addAlarmsItem(unmodifiedAlarm1);
        mockResponse.addAlarmsItem(modifiedAlarm3);
        mockResponse.setOffset(24L);
        Mockito.when(restClient.listAlarms()).thenReturn(mockResponse);

        Mockito.when(clock.currentTimeMillis()).thenReturn(NOW);

        fetchEngine.fetchAlarms();

        // Then

        LastApiPoll lastApiPollForAlarm = lastApiPollRepository.findLastApiPollForAlarm();
        assertEquals(NOW, lastApiPollForAlarm.getLastFullReadTime());
        assertEquals(LESS_THAN_ONE_DAY_BEFORE_NOW, lastApiPollForAlarm.getLastChangesPollTime());
        assertEquals(24L, lastApiPollForAlarm.getCurrentOffset());

        assertEquals(2, alarmRepository.findAll().size());
        assertEquals("alarm-1", alarmRepository.findById("1").getLabel());
        assertEquals("alarm-3-modified", alarmRepository.findById("3").getLabel());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void fetchAlarms_givenSomeAlarmsAlreadyInDatabase_whenChangesRead_thenDeletesRemovedAndUpdatesRest(
            boolean testDuplicateReads) {

        // Given

        Alarm existingAlarm1 = createAlarm("1", "alarm-1");
        Alarm existingAlarm2 = createAlarm("2", "alarm-2");
        Alarm existingAlarm3 = createAlarm("3", "alarm-3");

        alarmRepository.insertOrUpdate(existingAlarm1);
        alarmRepository.insertOrUpdate(existingAlarm2);
        alarmRepository.insertOrUpdate(existingAlarm3);

        LastApiPoll lastApiPoll = lastApiPollRepository.findLastApiPollForAlarm();
        lastApiPoll.setLastChangesPollTime(LESS_THAN_ONE_DAY_BEFORE_NOW);
        lastApiPoll.setLastFullReadTime(LESS_THAN_ONE_DAY_BEFORE_NOW); // Change read is enough
        lastApiPoll.setCurrentOffset(12L);

        lastApiPollRepository.updateLastApiPoll(lastApiPoll);

        // When

        // We simulate that alarm1 has not changed (alarms changes never report alarms as deleted)
        Alarm modifiedAlarm2 = createAlarm("2", "alarm-2-modified"); // <= note modified label
        Alarm modifiedAlarm3 = createAlarm("3", "alarm-3-modified"); // <= note modified label

        AlarmChangesResponse mockResponse = new AlarmChangesResponse();
        mockResponse.addAlarmsItem(modifiedAlarm2);
        mockResponse.addAlarmsItem(modifiedAlarm3);
        mockResponse.setOffset(24L);
        Mockito.when(restClient.getAlarmChangesAfterOffset(12L)).thenReturn(mockResponse);

        Mockito.when(clock.currentTimeMillis()).thenReturn(NOW);

        fetchEngine.fetchAlarms();

        if (testDuplicateReads) {
            // In some cases the API may return the same changes more than once
            // By calling the API twice while getting same result back both times, we verify that the engine can handle
            // duplicate changes
            mockResponse.setOffset(28L);
            Mockito.when(restClient.getAlarmChangesAfterOffset(24L))
                    .thenReturn(mockResponse); // Offset was updated by previous read
            Mockito.when(clock.currentTimeMillis()).thenReturn(JUST_AFTER_NOW); // Time has passed since now
            fetchEngine.fetchAlarms();
        }

        // Then

        LastApiPoll lastApiPollForAlarm = lastApiPollRepository.findLastApiPollForAlarm();
        assertEquals(LESS_THAN_ONE_DAY_BEFORE_NOW, lastApiPollForAlarm.getLastFullReadTime());
        assertEquals((testDuplicateReads ? JUST_AFTER_NOW : NOW), lastApiPollForAlarm.getLastChangesPollTime());
        assertEquals((testDuplicateReads ? 28L : 24L), lastApiPollForAlarm.getCurrentOffset());

        assertEquals(3, alarmRepository.findAll().size());
        assertEquals("alarm-1", alarmRepository.findById("1").getLabel());
        assertEquals("alarm-2-modified", alarmRepository.findById("2").getLabel());
        assertEquals("alarm-3-modified", alarmRepository.findById("3").getLabel());
    }

    private static Alarm createAlarm(String id, String label) {
        Alarm alarm = new Alarm();
        alarm.setId(id);
        alarm.setLabel(label);
        return alarm;
    }

    private static Organization createOrganization(String id, String label) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setLabel(label);
        return organization;
    }

    private static Location createLocation(String id, String label) {
        Location location = new Location();
        location.setId(id);
        location.setLabel(label);
        return location;
    }

    private static Device createDevice(String id, String label) {
        Device device = new Device();
        device.setId(id);
        device.setLabel(label);
        return device;
    }
}
