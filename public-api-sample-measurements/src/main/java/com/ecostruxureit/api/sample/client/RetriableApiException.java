/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

/**
 * Subclasses of this class are transient errors that may not occur when the same action is performed again later. Apply a reasonable back
 * off strategy to avoid getting rate limited.
 */
public abstract class RetriableApiException extends ApiException {

    public RetriableApiException(String message) {

        super(message);
    }

    public RetriableApiException(String message, Throwable cause) {

        super(message, cause);
    }
}
