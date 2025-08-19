/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The "entry point" of the application, which doesn't do anything other than starting up Spring.
 * <p>
 * After startup, the only actions taken by the program on its own, are initiated by the {@link FetchTimer}, that runs a background thread.
 * Spring also starts up web server, which can by default be reached on {@code http://localhost:8080}.
 */
@SpringBootApplication
public class PublicApiSampleClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicApiSampleClientApplication.class, args);
    }
}
