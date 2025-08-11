package com.ecostruxureit.api.sample.client;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
public final class UnexpectedStatusCodeApiException extends ApiException {

    public UnexpectedStatusCodeApiException(int statusCode) {

        super("Unexpected status code was received: " + statusCode);
    }
}
