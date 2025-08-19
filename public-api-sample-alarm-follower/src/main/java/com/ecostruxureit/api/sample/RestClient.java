/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import generated.dto.AlarmChangesResponse;
import generated.dto.GetDeviceResponse;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The "entry point" of the application, which doesn't do anything other than starting up Spring.
 * <p>
 * After startup, the only actions taken by the program, are initiated by the {@link FetchTimer}, that runs a background thread.
 */
@Service
public class RestClient {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration READ_TIMEOUT = Duration.ofMinutes(2);

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);

    private final Configuration configuration;
    private final RestTemplate restTemplate;

    RestClient(Configuration configuration, RestTemplateBuilder restTemplateBuilder) {
        this.configuration = Objects.requireNonNull(configuration);
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setReadTimeout(READ_TIMEOUT)
                .build();
    }

    GetDeviceResponse getDevice(String deviceId) {

        try {
            return httpGet(GetDeviceResponse.class, "inventory", deviceId);

        } catch (HttpClientErrorException.NotFound e) {
            LOGGER.warn("Device with id {} was not found", deviceId);
            return null;
        }
    }

    AlarmChangesResponse getAlarmChangesAfterOffset(long offset) {

        return httpGet(AlarmChangesResponse.class, "alarm-changes", Long.toString(offset));
    }

    private <T> T httpGet(Class<T> expectedResultType, String... extraPathSegments) {

        String uri = UriComponentsBuilder.fromUriString(configuration.getApiUrl())
                .pathSegment("organizations", configuration.getOrganizationId())
                .pathSegment(extraPathSegments)
                .toUriString();

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set(HttpHeaders.AUTHORIZATION, "bearer " + configuration.getApiKey());
        HttpEntity<Void> requestEntity = new HttpEntity<>(null, requestHeaders);

        ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, expectedResultType);

        // The call to exchange above already throws an exception if receiving 4xx and 5xx status codes.
        // But we only see "200 OK" as success, so throw exception if it isn't (e.g. if it is "204 No Content").
        if (!HttpStatus.OK.equals(response.getStatusCode())) {
            throw new RuntimeException("Request failed with status: " + response.getStatusCode());
        }

        return response.getBody();
    }
}
