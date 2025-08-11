package com.ecostruxureit.api.sample;

import jakarta.annotation.PostConstruct;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Creates a background thread, that continuously tells the {@link FetchEngine} to retrieve inventory objects and alarms from the REST API.
 * <p>
 * Note that we don't create an instance of this class when running tests as it would interfere with our tests if it starts a thread that
 * fetches data on its own. See {@link Profiles} for more info.
 */
@Service
@Profile(Profiles.NOT_TEST)
public class FetchTimer {

    /**
     * How long the timer should sleep between asking the {@link FetchEngine} to retrieve inventory objects and alarms. Our recommendation
     * is to poll every 10 seconds, which strikes a good balance between getting updates pretty fast, without being rate limited by the API.
     */
    private static final int SLEEP_MILLIS_BETWEEN_FETCHES = 10_000;

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchTimer.class);

    private final FetchEngine fetchEngine;

    FetchTimer(FetchEngine fetchEngine) {
        this.fetchEngine = Objects.requireNonNull(fetchEngine);
    }

    @PostConstruct
    void startTimer() {

        // Note: One could run 2 threads instead of the one below - one to fetch alarms, and another to fetch inventory
        // objects.
        //
        // If you want to retrieve data from multiple organizations, then it might make sense to fetch data using one
        // thread per
        // organization.
        //
        // Warning: Do NOT use multiple threads to fetch alarms nor multiple threads to fetch inventory objects for a
        // specific organization,
        // as it will result in corrupted data because of race conditions (changes to your data must be applied in the
        // correct order).
        Thread timerThread = new Thread(() -> {
            while (true) {
                try {
                    fetchEngine.fetchInventoryObjects();
                } catch (Exception e) {
                    LOGGER.error("Failed fetching inventory objects", e);
                }
                try {
                    fetchEngine.fetchAlarms();
                } catch (Exception e) {
                    LOGGER.error("Failed fetching alarms", e);
                }
                try {
                    Thread.sleep(SLEEP_MILLIS_BETWEEN_FETCHES);
                } catch (InterruptedException ignored) {
                }
            }
        });
        timerThread.start();
    }
}
