/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

public final class UnknownRemoteRetriableApiException extends RetriableApiException {

    public UnknownRemoteRetriableApiException() {

        super("An unknown error occurred within the EcoStruxure IT API, please try again later");
    }
}
