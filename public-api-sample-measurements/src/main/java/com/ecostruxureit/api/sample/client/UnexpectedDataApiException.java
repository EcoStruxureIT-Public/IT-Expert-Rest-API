package com.ecostruxureit.api.sample.client;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
public final class UnexpectedDataApiException extends ApiException {

    public UnexpectedDataApiException(String line) {

        super("Unexpected data was received: " + line);
    }
}
