package com.ecostruxureit.api.sample.client;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
public final class UnknownLocalRetriableApiException extends RetriableApiException {

    public UnknownLocalRetriableApiException(Throwable cause) {

        super("An unknown error occurred, please try again later", cause);
    }
}
