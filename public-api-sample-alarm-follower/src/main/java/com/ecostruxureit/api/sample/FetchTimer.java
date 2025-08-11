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
 * Creates a background thread, that continuously tells the {@link FetchEngine} to retrieve alarm changes from the REST API.
 * <p>
 * Note that we don't create an instance of this class when running tests as it would interfere with our tests if it starts a thread that
 * fetches data on its own.
 */
@Service
@Profile(Profiles.NOT_TEST)
public class FetchTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchTimer.class);

    private final FetchEngine fetchEngine;

    FetchTimer(FetchEngine fetchEngine) {
        this.fetchEngine = Objects.requireNonNull(fetchEngine);
    }

    @PostConstruct
    void startTimer() {
        Thread timerThread = new Thread(() -> {
            while (true) {
                try {
                    fetchEngine.fetchAlarms();
                } catch (Exception e) {
                    LOGGER.error("Failed fetching alarms", e);
                }
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ignored) {
                }
            }
        });
        timerThread.start();
    }
}
