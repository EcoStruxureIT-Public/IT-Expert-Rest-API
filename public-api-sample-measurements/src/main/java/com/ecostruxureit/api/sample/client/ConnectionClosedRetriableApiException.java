package com.ecostruxureit.api.sample.client;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
public final class ConnectionClosedRetriableApiException extends RetriableApiException {

    public ConnectionClosedRetriableApiException() {

        super("Connection was closed, please try again later");
    }
}
