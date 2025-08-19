/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

public final class UnavailableRetriableApiException extends RetriableApiException {

    public UnavailableRetriableApiException() {

        super("The EcoStruxure IT API is currently unavailable, please try again later");
    }
}
