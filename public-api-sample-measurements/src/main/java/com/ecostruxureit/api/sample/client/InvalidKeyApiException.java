package com.ecostruxureit.api.sample.client;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
public final class InvalidKeyApiException extends ApiException {

    public InvalidKeyApiException() {

        super("The provided API key is invalid");
    }
}
