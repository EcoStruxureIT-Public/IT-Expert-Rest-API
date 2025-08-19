/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

public final class RateLimitRetriableApiException extends RetriableApiException {

    public RateLimitRetriableApiException() {

        super("Your have performed too many calls, please try again later");
    }
}
