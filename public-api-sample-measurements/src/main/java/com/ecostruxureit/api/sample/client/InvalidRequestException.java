/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

public final class InvalidRequestException extends ApiException {

    public InvalidRequestException() {

        super("The request is invalid");
    }
}
