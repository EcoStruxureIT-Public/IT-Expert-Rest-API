/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

public final class ConnectionClosedRetriableApiException extends RetriableApiException {

    public ConnectionClosedRetriableApiException() {

        super("Connection was closed, please try again later");
    }
}
