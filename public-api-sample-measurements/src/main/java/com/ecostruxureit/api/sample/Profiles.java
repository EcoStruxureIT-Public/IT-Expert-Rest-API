package com.ecostruxureit.api.sample;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Defines the "modes" in which the application can be run - which is basically in "test" and "not test".
 * <p>
 * All tests are annotated with {@code @ActiveProfiles(Profiles.TEST)}, which means test mode is enabled when they run.
 * <p>
 * The class {@link FetchTimer} is annotated with {@code @Profile(Profiles.NOT_TEST)}, which means Spring will NOT create an instance of
 * this class, if running in test mode (which means it will not start a background thread).
 * <p>
 * If running in test mode, then {@code application-test.properties} is used instead of {@code application.properties}.
 */
public final class Profiles {

    public static final String TEST = "test";

    public static final String NOT_TEST = "!test";

    private Profiles() {}
}
