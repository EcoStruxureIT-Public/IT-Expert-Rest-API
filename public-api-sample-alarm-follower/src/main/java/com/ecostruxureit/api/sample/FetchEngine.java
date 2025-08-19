/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import generated.dto.Alarm;
import generated.dto.AlarmChangesResponse;
import generated.dto.Device;
import generated.dto.GetDeviceResponse;
import generated.dto.InventoryObject;
import generated.dto.Location;
import generated.dto.Organization;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Responsible for fetching alarm changes (and the devices on which they happened) by using the {@link RestClient}.
 * <p>
 * Writes retrieved alarm changes to the console.
 * <p>
 * Is continually triggered by {@link FetchTimer} as long as the program is running.
 * <p>
 * Spring creates a singleton instance of this class, so there is just a single copy of this field in the application.
 * <p>
 * In a real application it would make sense to save the offset somewhere, so the application can continue where it
 * left off after a restart.
 */
@Service
public class FetchEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchEngine.class);

    private final RestClient restClient;

    private long currentOffset;

    FetchEngine(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient);
    }

    void fetchAlarms() {
        LOGGER.info("Fetching new and updated alarms from offset: {}", currentOffset);

        AlarmChangesResponse alarmChangesResponse = restClient.getAlarmChangesAfterOffset(currentOffset);

        // Figure out which devices are referenced by the retrieved alarms (no need to fetch each of them more than
        // once)
        Set<String> referencedDeviceIds = new HashSet<>();
        for (Alarm alarm : alarmChangesResponse.getAlarms()) {
            referencedDeviceIds.add(alarm.getDeviceId());
        }

        // Fetch all the devices that are referenced by one or more alarms
        Map<String, GetDeviceResponse> deviceIdToGetDeviceResponse = new HashMap<>();
        for (String referencedDeviceId : referencedDeviceIds) {
            GetDeviceResponse getDeviceResponse = restClient.getDevice(referencedDeviceId);
            deviceIdToGetDeviceResponse.put(referencedDeviceId, getDeviceResponse);
        }

        // Do something interesting with the alarms... here we just write some info about the alarm and the device on
        // which it occurred
        for (Alarm alarm : alarmChangesResponse.getAlarms()) {
            GetDeviceResponse getDeviceResponse = deviceIdToGetDeviceResponse.get(alarm.getDeviceId());

            LOGGER.info(
                    "New/Updated alarm: {} {} > {}",
                    alarm.getSeverity(),
                    alarm.getLabel(),
                    createDescriptionWithAncestors(getDeviceResponse));
        }

        if (currentOffset != alarmChangesResponse.getOffset()) {
            currentOffset = alarmChangesResponse.getOffset();
            LOGGER.info("Updated offset to {}", currentOffset);
        }
    }

    private static String createDescriptionWithAncestors(GetDeviceResponse getDeviceResponse) {
        if (getDeviceResponse == null) {
            return null;
        }
        StringBuilder description = new StringBuilder();
        description.append(createDescription(getDeviceResponse.getDevice()));
        for (InventoryObject ancestor : getDeviceResponse.getAncestors()) {

            description.append(" > ");

            if (ancestor instanceof Device device) {
                description.append(createDescription(device));

            } else if (ancestor instanceof Location location) {
                description.append(createDescription(location));

            } else if (ancestor instanceof Organization organization) {
                description.append(createDescription(organization));
            }
        }

        return description.toString();
    }

    private static String createDescription(Device device) {
        return device.getLabel() + " (" + device.getType() + ")";
    }

    private static String createDescription(Location location) {
        return location.getLabel() + " (" + location.getType() + ")";
    }

    private static String createDescription(Organization organization) {
        return organization.getLabel() + " (Organization)";
    }
}
