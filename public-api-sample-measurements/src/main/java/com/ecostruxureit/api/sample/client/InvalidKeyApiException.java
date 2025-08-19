/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

public final class InvalidKeyApiException extends ApiException {

    public InvalidKeyApiException() {

        super("The provided API key is invalid");
    }
}
