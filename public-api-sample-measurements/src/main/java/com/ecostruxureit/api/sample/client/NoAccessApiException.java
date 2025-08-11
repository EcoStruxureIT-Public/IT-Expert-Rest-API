package com.ecostruxureit.api.sample.client;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
public final class NoAccessApiException extends ApiException {

    public NoAccessApiException() {

        super("The provided API key does not grant access to the requested organization");
    }
}
