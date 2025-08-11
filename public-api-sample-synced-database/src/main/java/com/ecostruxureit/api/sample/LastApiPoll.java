package com.ecostruxureit.api.sample;

import java.time.Instant;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Two instances of this object is stored in the database in the {@code last_api_poll} table.
 * <p>
 * The instances each contain information about when the data was last retrieved from the REST endpoint - one about when alarms were last
 * retrieved and one about when inventory objects were last retrieved.
 */
class LastApiPoll {

    static final String RESOURCE_NAME_FOR_ALARM = "alarm";
    static final String RESOURCE_NAME_FOR_INVENTORY_OBJECT = "inventory_object";

    private String resourceName;
    private long currentOffset;
    private long lastPollTime;
    private long lastFullReadTime;

    /**
     * @return either {@link #RESOURCE_NAME_FOR_ALARM} or {@link #RESOURCE_NAME_FOR_INVENTORY_OBJECT} depending on which this instance
     * contains poll information about.
     */
    String getResourceName() {
        return resourceName;
    }

    void setResourceName(String resourceName) {
        if (!(RESOURCE_NAME_FOR_ALARM.equals(resourceName)
                || RESOURCE_NAME_FOR_INVENTORY_OBJECT.equals(resourceName))) {
            throw new IllegalArgumentException("Invalid value for resourceName: " + resourceName);
        }
        this.resourceName = resourceName;
    }

    /**
     * @return the offset that was returned from the REST API the last time data were fetched.
     */
    long getCurrentOffset() {
        return currentOffset;
    }

    void setCurrentOffset(long currentOffset) {
        this.currentOffset = currentOffset;
    }

    /**
     * @return the last time (based on {@link Clock#currentTimeMillis()}) that changes were retrieved for the given resource.
     */
    long getLastChangesPollTime() {
        return lastPollTime;
    }

    /**
     * @param lastPollChangesTime should be based on {@link Clock#currentTimeMillis()}
     */
    void setLastChangesPollTime(long lastPollChangesTime) {
        this.lastPollTime = lastPollChangesTime;
    }

    /**
     * @return the last time (based on {@link Clock#currentTimeMillis()}) that a full data set were retrieved for the given resource.
     */
    long getLastFullReadTime() {
        return lastFullReadTime;
    }

    /**
     * @param lastFullReadTime should be based on {@link Clock#currentTimeMillis()}
     */
    void setLastFullReadTime(long lastFullReadTime) {
        this.lastFullReadTime = lastFullReadTime;
    }

    @Override
    public String toString() {
        return "LastApiPoll{" + "resourceName='"
                + resourceName + '\'' + ", currentOffset="
                + currentOffset + ", lastPollTime="
                + Instant.ofEpochMilli(lastPollTime) + ", lastFullReadTime="
                + Instant.ofEpochMilli(lastFullReadTime) + '}';
    }
}
