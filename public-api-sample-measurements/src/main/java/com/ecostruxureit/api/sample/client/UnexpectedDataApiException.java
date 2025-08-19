/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

public final class UnexpectedDataApiException extends ApiException {

    public UnexpectedDataApiException(String line) {

        super("Unexpected data was received: " + line);
    }
}
