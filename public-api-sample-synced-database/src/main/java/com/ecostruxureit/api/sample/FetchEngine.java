package com.ecostruxureit.api.sample;

import generated.dto.Alarm;
import generated.dto.AlarmChangesResponse;
import generated.dto.AlarmsResponse;
import generated.dto.InventoryChangesResponse;
import generated.dto.InventoryObject;
import generated.dto.InventoryObjectChange;
import generated.dto.InventoryObjectCreateOrUpdate;
import generated.dto.InventoryObjectDelete;
import generated.dto.InventoryResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Retrieves alarms and inventory objects and decides when to do a "full" read, and when just reading recent changes are enough. Also stores
 * the retrieved data into a database.
 * <p>
 * Is continuously triggered by {@link FetchTimer} for as long as the program is running.
 */
@Service
public class FetchEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchEngine.class);

    /**
     * If the duration since last doing a FULL read of all alarms / inventory objects from the API has exceeded this duration, then the
     * engine will do a full read. This shouldn't be required in theory, but it is good practice to do a full read every now and then just
     * to bring the data in sync (e.g. if they had diverged because of a bug).
     */
    private static final long MAX_MILLIS_SINCE_LAST_FULL_READ_BEFORE_REQUIRING_FULL_READ =
            Duration.ofDays(7).toMillis();

    /**
     * The API does not support reading CHANGES too far back in time (a couple of days only). In other words, if for example you haven't
     * done a FULL or CHANGES read for two weeks, and then do a CHANGES read, then you will most likely end up missing some of the changes
     * that has happened since then (you won't get an error from the API - some data will just be missing).
     * <p>
     * Therefore, if the last time the engine did a CHANGE or FULL read is fairly recent, then the engine will only do a CHANGE read, but if
     * it is too long ago, then the engine will do a FULL read instead. This constant defines the max time since last CHANGE or FULL read
     * before the engine will choose a FULL read instead of just a CHANGE read.
     */
    private static final long MAX_MILLIS_SINCE_LAST_READ_BEFORE_REQUIRING_FULL_READ =
            Duration.ofDays(1).toMillis();

    private final AlarmRepository alarmRepository;
    private final Clock clock;
    private final InventoryObjectRepository inventoryObjectRepository;
    private final LastApiPollRepository lastApiPollRepository;
    private final RestClient restClient;

    FetchEngine(
            AlarmRepository alarmRepository,
            Clock clock,
            InventoryObjectRepository inventoryObjectRepository,
            LastApiPollRepository lastApiPollRepository,
            RestClient restClient) {

        this.alarmRepository = Objects.requireNonNull(alarmRepository);
        this.clock = Objects.requireNonNull(clock);
        this.inventoryObjectRepository = Objects.requireNonNull(inventoryObjectRepository);
        this.lastApiPollRepository = Objects.requireNonNull(lastApiPollRepository);
        this.restClient = Objects.requireNonNull(restClient);
    }

    // Ensures that all updates to inventory objects and the update of last api poll happens in same transaction
    @Transactional(propagation = Propagation.REQUIRED)
    void fetchInventoryObjects() {

        long now = clock.currentTimeMillis();
        LastApiPoll lastApiPoll = lastApiPollRepository.findLastApiPollForInventoryObject();
        LOGGER.debug("Loaded last API poll info {}", lastApiPoll);

        if (shouldDoFullRead(now, lastApiPoll)) {

            LOGGER.info("Doing full inventory read based on poll info {}", lastApiPoll);

            InventoryResponse inventoryResponse = restClient.listInventory();
            List<InventoryObject> inventoryObjects = inventoryResponse.getInventoryObjects();
            Set<String> inventoryIds = Objects.requireNonNull(inventoryObjects).stream()
                    .map(InventoryObject::getId)
                    .collect(Collectors.toSet());

            Set<String> allIdsOnlyInOurDatabase = inventoryObjectRepository.findAllIds();
            allIdsOnlyInOurDatabase.removeAll(inventoryIds);
            if (!allIdsOnlyInOurDatabase.isEmpty()) {
                inventoryObjectRepository.deleteByIds(allIdsOnlyInOurDatabase);
                LOGGER.info("Deleted {} obsolete inventory objects", allIdsOnlyInOurDatabase.size());
            }

            for (InventoryObject inventoryObject : inventoryObjects) {
                // Note: We just replace all the existing objects - one could of course only replace the ones that have
                // actually changed.
                inventoryObjectRepository.insertOrUpdate(inventoryObject);
            }
            LOGGER.info("Updated {} inventory objects", inventoryObjects.size());

            lastApiPoll.setLastFullReadTime(now);
            lastApiPoll.setCurrentOffset(inventoryResponse.getOffset());

        } else {

            LOGGER.info("Doing changed inventory objects read based on poll info {}", lastApiPoll);

            // Note: If there are a LOT of changes since the last poll (won't happen very often if you poll frequently),
            // then the API might
            // not send you all of them right away. In that case you will just get the changes on a later call.
            //
            // Therefore, in situation where you do get changes, you could do another poll straight away, until you no
            // longer get any new
            // changes, and then do a longer sleep after that. But it will complicate the code, so we wouldn't recommend
            // doing it.

            InventoryChangesResponse inventoryChangesResponse =
                    restClient.getInventoryChangesAfterOffset(lastApiPoll.getCurrentOffset());
            for (InventoryObjectChange inventoryObjectChange :
                    Objects.requireNonNull(inventoryChangesResponse.getInventoryObjectChanges())) {

                if (inventoryObjectChange instanceof InventoryObjectDelete inventoryObjectDelete) {
                    inventoryObjectRepository.deleteById(inventoryObjectDelete.getInventoryObjectId());

                } else {
                    InventoryObjectCreateOrUpdate createOrUpdate =
                            (InventoryObjectCreateOrUpdate) inventoryObjectChange;
                    InventoryObject inventoryObject = createOrUpdate.getInventoryObject();
                    inventoryObjectRepository.insertOrUpdate(inventoryObject);
                }
            }

            lastApiPoll.setLastChangesPollTime(now);
            lastApiPoll.setCurrentOffset(inventoryChangesResponse.getOffset());
        }
        lastApiPollRepository.updateLastApiPoll(lastApiPoll);
        LOGGER.debug("Updated poll info to {}", lastApiPoll);
    }

    // Ensures that all updates to alarms and the update of last api poll happens in same transaction
    @Transactional(propagation = Propagation.REQUIRED)
    void fetchAlarms() {
        long now = clock.currentTimeMillis();
        LastApiPoll lastApiPoll = lastApiPollRepository.findLastApiPollForAlarm();
        LOGGER.debug("Loaded last API poll info {}", lastApiPoll);

        if (shouldDoFullRead(now, lastApiPoll)) {

            LOGGER.info("Doing full alarm read based on poll info {}", lastApiPoll);

            AlarmsResponse alarmsResponse = restClient.listAlarms();
            List<Alarm> alarms = alarmsResponse.getAlarms();
            Set<String> alarmIds =
                    Objects.requireNonNull(alarms).stream().map(Alarm::getId).collect(Collectors.toSet());

            Set<String> allIdsOnlyInOurDatabase = alarmRepository.findAllIds();
            allIdsOnlyInOurDatabase.removeAll(alarmIds);
            if (!allIdsOnlyInOurDatabase.isEmpty()) {

                // Note: The alarm change API never marks an alarm as deleted - alarms just get a timestamp value in
                // clearedTime.
                // On the other hand, when reading the "full" alarm situation from the API, then old cleared alarms WILL
                // have been removed
                // (the API returns all alarms that are still active, but only the cleared alarms that have cleared
                // within the last week).
                //
                // Below we just remove alarms from our local database, that are no longer returned from the API, which
                // mean that our
                // database will also contain all active alarms and all alarms that has cleared no more than a week ago.
                //
                // If you want to keep alarms longer (or forever), then instead of deleting alarms no longer returned
                // from the REST API, you
                // should instead just verify that they are at least cleared (they must be cleared - or else they would
                // still be returned
                // from the API as all active alarms that are still active are returned).
                alarmRepository.deleteByIds(allIdsOnlyInOurDatabase);
                LOGGER.info("Deleted {} obsolete alarms", allIdsOnlyInOurDatabase.size());
            }

            for (Alarm alarm : alarms) {

                // Note: We just replace all the existing alarms - one could of course only replace the ones that have
                // actually changed.
                alarmRepository.insertOrUpdate(alarm);
            }
            LOGGER.info("Updated {} alarms", alarms.size());

            lastApiPoll.setLastFullReadTime(now);
            lastApiPoll.setCurrentOffset(alarmsResponse.getOffset());

        } else {

            LOGGER.info("Doing changed alarm read based on poll info {}", lastApiPoll);

            AlarmChangesResponse alarmChangesResponse =
                    restClient.getAlarmChangesAfterOffset(lastApiPoll.getCurrentOffset());
            for (Alarm alarm : Objects.requireNonNull(alarmChangesResponse.getAlarms())) {
                alarmRepository.insertOrUpdate(alarm);
            }

            lastApiPoll.setLastChangesPollTime(now);
            lastApiPoll.setCurrentOffset(alarmChangesResponse.getOffset());
        }
        lastApiPollRepository.updateLastApiPoll(lastApiPoll);
        LOGGER.debug("Updated poll info to {}", lastApiPoll);
    }

    static boolean shouldDoFullRead(long now, LastApiPoll lastApiPoll) {

        if (now > (lastApiPoll.getLastFullReadTime() + MAX_MILLIS_SINCE_LAST_FULL_READ_BEFORE_REQUIRING_FULL_READ)) {
            return true;
        }

        long lastReadTime = Math.max(lastApiPoll.getLastChangesPollTime(), lastApiPoll.getLastFullReadTime());
        return now > (lastReadTime + MAX_MILLIS_SINCE_LAST_READ_BEFORE_REQUIRING_FULL_READ);
    }
}
