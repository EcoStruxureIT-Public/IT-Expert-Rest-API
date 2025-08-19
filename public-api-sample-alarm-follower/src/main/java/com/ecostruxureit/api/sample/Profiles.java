/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

/**
 * Defines the "modes" in which the application can be run - which is basically in "test" and "not test".
 * <p>
 * All tests are annotated with {@code @ActiveProfiles(Profiles.TEST)}, which means test mode is enabled when they run.
 * The class {@link FetchTimer} is annotated with {@code @Profile(Profiles.NOT_TEST)}, which means Spring will NOT create an instance of
 * this class, if running in test mode (which means it will not start a background thread).
 */
class Profiles {

    static final String TEST = "test";
    static final String NOT_TEST = "!test";
}
