package com.ecostruxureit.api.sample.client;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
public final class InvalidRequestException extends ApiException {

    public InvalidRequestException() {

        super("The request is invalid");
    }
}
