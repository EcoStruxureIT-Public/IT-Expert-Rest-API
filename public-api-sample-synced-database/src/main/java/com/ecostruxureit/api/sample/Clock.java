package com.ecostruxureit.api.sample;

import org.springframework.stereotype.Service;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Reading of the clock is wrapped in this class to be able to use fixed values in tests.
 */
@Service
class Clock {

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
