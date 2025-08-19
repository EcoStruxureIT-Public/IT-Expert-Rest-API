/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

public final class UnexpectedStatusCodeApiException extends ApiException {

    public UnexpectedStatusCodeApiException(int statusCode) {

        super("Unexpected status code was received: " + statusCode);
    }
}
