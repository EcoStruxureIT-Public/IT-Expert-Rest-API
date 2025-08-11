package com.ecostruxureit.api.sample.client;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
public final class UnknownRemoteRetriableApiException extends RetriableApiException {

    public UnknownRemoteRetriableApiException() {

        super("An unknown error occurred within the EcoStruxure IT API, please try again later");
    }
}
