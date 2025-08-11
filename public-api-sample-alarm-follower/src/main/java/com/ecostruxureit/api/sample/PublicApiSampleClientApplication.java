package com.ecostruxureit.api.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * The "entry point" of the application, which doesn't do anything other than starting up Spring.
 * <p>
 * After startup, the only actions taken by the program, are initiated by the {@link FetchTimer}, that runs a background thread.
 */
@SpringBootApplication
public class PublicApiSampleClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicApiSampleClientApplication.class, args);
    }
}
