package com.ecostruxureit.api.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Tests the methods in {@link LastApiPollRepository}.
 * <p>
 * Using the {@link Transactional} annotation on a test class means that test methods will each be run in a separate transaction, which is
 * rolled back, when the test finishes - which means test methods won't "pollute" the database with data that may make other tests fail.
 */
@SpringBootTest
@ActiveProfiles(Profiles.TEST)
@Transactional
class LastApiPollRepositoryTest {

    @Autowired
    private LastApiPollRepository lastApiPollRepository;

    @Test
    void initialLastApiPollForAlarm() {

        LastApiPoll initialLastApiPollForAlarm = lastApiPollRepository.findLastApiPollForAlarm();

        assertEquals("alarm", initialLastApiPollForAlarm.getResourceName());
        assertEquals(0L, initialLastApiPollForAlarm.getCurrentOffset());
        assertEquals(0L, initialLastApiPollForAlarm.getLastChangesPollTime());
        assertEquals(0L, initialLastApiPollForAlarm.getLastFullReadTime());
    }

    @Test
    void initialLastApiPollForInventoryObject() {

        LastApiPoll initialLastApiPollForInventoryObject = lastApiPollRepository.findLastApiPollForInventoryObject();

        assertEquals("inventory_object", initialLastApiPollForInventoryObject.getResourceName());
        assertEquals(0L, initialLastApiPollForInventoryObject.getCurrentOffset());
        assertEquals(0L, initialLastApiPollForInventoryObject.getLastChangesPollTime());
        assertEquals(0L, initialLastApiPollForInventoryObject.getLastFullReadTime());
    }

    @Test
    void updateLastApiPollForAlarm() {

        LastApiPoll lastApiPollForAlarm = lastApiPollRepository.findLastApiPollForAlarm();

        long updatedLastChangesPollTime = 1568200461360L;
        long updatedCurrentOffset = 34;
        long updatedLastFullReadTime = 35;

        lastApiPollForAlarm.setLastChangesPollTime(updatedLastChangesPollTime);
        lastApiPollForAlarm.setLastFullReadTime(updatedLastFullReadTime);
        lastApiPollForAlarm.setCurrentOffset(updatedCurrentOffset);

        lastApiPollRepository.updateLastApiPoll(lastApiPollForAlarm);

        LastApiPoll lastApiPollForAlarmFromRepository = lastApiPollRepository.findLastApiPollForAlarm();
        assertEquals("alarm", lastApiPollForAlarmFromRepository.getResourceName());
        assertEquals(updatedCurrentOffset, lastApiPollForAlarmFromRepository.getCurrentOffset());
        assertEquals(updatedLastChangesPollTime, lastApiPollForAlarmFromRepository.getLastChangesPollTime());
        assertEquals(updatedLastFullReadTime, lastApiPollForAlarmFromRepository.getLastFullReadTime());
    }

    @Test
    void updateLastApiPollForInventoryObject() {

        LastApiPoll lastApiPollForInventoryObject = lastApiPollRepository.findLastApiPollForInventoryObject();

        long updatedLastPollTime = 1568200478598L;
        long updatedCurrentOffset = 2345;
        long updatedLastFullReadTime = 6789;

        lastApiPollForInventoryObject.setLastChangesPollTime(updatedLastPollTime);
        lastApiPollForInventoryObject.setCurrentOffset(updatedCurrentOffset);
        lastApiPollForInventoryObject.setLastFullReadTime(updatedLastFullReadTime);

        lastApiPollRepository.updateLastApiPoll(lastApiPollForInventoryObject);

        LastApiPoll lastApiPollForInventoryObjectFromRepository =
                lastApiPollRepository.findLastApiPollForInventoryObject();
        assertEquals("inventory_object", lastApiPollForInventoryObjectFromRepository.getResourceName());
        assertEquals(updatedCurrentOffset, lastApiPollForInventoryObjectFromRepository.getCurrentOffset());
        assertEquals(updatedLastPollTime, lastApiPollForInventoryObjectFromRepository.getLastChangesPollTime());
        assertEquals(updatedLastFullReadTime, lastApiPollForInventoryObjectFromRepository.getLastFullReadTime());
    }
}
