package com.ecostruxureit.api.sample.client;

import com.ecostruxureit.api.sample.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import generated.dto.CompletedReplaySystemMessage;
import generated.dto.Measurement;
import generated.dto.RateLimitReachedReplaySystemMessage;
import generated.dto.ReplaySystemMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
@Service
public class ApiClient {

    @FunctionalInterface
    public interface EndStreamPredicate {

        boolean test(String line) throws ApiException;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiClient.class);

    private static final int CONNECT_TIMEOUT_IN_MILLISECONDS = 5_000;

    /**
     * The read timeout in milliseconds. Note that {@code 0} means infinite.
     *
     * @see HttpURLConnection#setReadTimeout(int)
     */
    private static final int READ_TIMEOUT_IN_MILLISECONDS = 0;

    private final Configuration configuration;

    private final ObjectMapper objectMapper;

    ApiClient(Configuration configuration, ObjectMapper objectMapper) {

        this.configuration = Objects.requireNonNull(configuration);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public void retrieveLiveMeasurements(Consumer<Measurement> consumer) throws ApiException {

        EndStreamPredicate endStreamPredicate = line -> false;

        stream("live", consumer, endStreamPredicate);
    }

    public void replayMeasurements(String fromOffset, String toOffset, Consumer<Measurement> consumer)
            throws ApiException {

        EndStreamPredicate endStreamPredicate = line -> {
            try {
                ReplaySystemMessage replaySystemMessage = objectMapper.readValue(line, ReplaySystemMessage.class);

                if (replaySystemMessage == null || replaySystemMessage.getReplaySystemMessageType() == null) {
                    return false;
                }

                if (replaySystemMessage instanceof CompletedReplaySystemMessage) {
                    LOGGER.debug("Received completed replay system message");
                    return true;
                }

                if (replaySystemMessage instanceof RateLimitReachedReplaySystemMessage) {
                    LOGGER.debug("Received rate limit reached replay system message");
                    throw new RateLimitRetriableApiException();
                }

                LOGGER.warn(
                        "Received unknown replay system message: {}", replaySystemMessage.getReplaySystemMessageType());

            } catch (JsonProcessingException ignored) {
            }

            return false;
        };

        // Note that the offsets returned by the API are guaranteed to be URL safe so we can send them directly back to
        // the API.

        stream("replay?fromOffset=" + fromOffset + "&toOffset=" + toOffset, consumer, endStreamPredicate);
    }

    private void stream(String path, Consumer<Measurement> consumer, EndStreamPredicate endStreamPredicate)
            throws ApiException {

        try {
            URL url = new URL(configuration.getApiUrl() + "/organizations/" + configuration.getOrganizationId()
                    + "/measurements/" + path);

            HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();

            httpUrlConnection.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLISECONDS);
            httpUrlConnection.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
            httpUrlConnection.setRequestMethod("GET");
            httpUrlConnection.setRequestProperty(HttpHeaders.ACCEPT_ENCODING, "gzip");
            httpUrlConnection.setRequestProperty(HttpHeaders.AUTHORIZATION, "bearer " + configuration.getApiKey());
            httpUrlConnection.setInstanceFollowRedirects(false);
            httpUrlConnection.setUseCaches(false);

            switch (httpUrlConnection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                    // This is the normal case where everything is okay and we can start streaming.
                    stream(httpUrlConnection.getInputStream(), consumer, endStreamPredicate);
                    break;

                case HttpURLConnection.HTTP_BAD_REQUEST:
                    throw new InvalidRequestException();

                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    throw new InvalidKeyApiException();

                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new NoAccessApiException();

                case 429: // Too Many Requests
                    throw new RateLimitRetriableApiException();

                case HttpURLConnection.HTTP_INTERNAL_ERROR:
                    throw new UnknownRemoteRetriableApiException();

                case HttpURLConnection.HTTP_UNAVAILABLE:
                    throw new UnavailableRetriableApiException();

                default:
                    // Includes HttpURLConnection.HTTP_NOT_ACCEPTABLE (406) which only happens if there is a programming
                    // error since we must
                    // always set the "Accept-Encoding" header to "gzip".
                    throw new UnexpectedStatusCodeApiException(httpUrlConnection.getResponseCode());
            }
        } catch (IOException ex) {
            throw new UnknownLocalRetriableApiException(ex);
        }
    }

    private void stream(InputStream inputStream, Consumer<Measurement> consumer, EndStreamPredicate endStreamPredicate)
            throws ApiException {

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            while (true) {

                String line = bufferedReader.readLine();

                if (line == null) {
                    throw new ConnectionClosedRetriableApiException();
                }

                if (line.isEmpty()) {
                    // Ignoring heartbeat.
                    continue;
                }

                try {
                    Measurement measurement = objectMapper.readValue(line, Measurement.class);
                    // If a line cannot be converted to a measurement, the measurement instance will just contain null
                    // for all its
                    // properties. Because of that we can use a property known to be non-null for measurements (sensor
                    // ID) to determine
                    // whether or not the line was a measurement.
                    if (measurement.getSensorId() != null) {
                        consumer.accept(measurement);
                        continue;
                    }
                } catch (JsonProcessingException ignored) {
                }

                if (endStreamPredicate.test(line)) {
                    return;
                }

                throw new UnexpectedDataApiException(line);
            }
        } catch (IOException ex) {
            throw new UnknownLocalRetriableApiException(ex);
        }
    }
}
