/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

/**
 * Subclasses of this class (except subclasses of {@link RetriableApiException}) are unrecoverable errors.
 */
public abstract class ApiException extends Exception {

    ApiException(String message) {

        super(message);
    }

    ApiException(String message, Throwable cause) {

        super(message, cause);
    }
}
