/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.client;

import static org.mockito.Mockito.when;

import com.ecostruxureit.api.sample.Configuration;
import com.ecostruxureit.api.sample.Profiles;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Makes requests to the real REST API, so turned off via the @Disabled annotation to prevent it from being run during builds of the
 * project.
 * <p>
 * To use it you must create a file in your home directory called {@code ApiClientTest.properties} with the following content:
 * <pre>
 * organizationId=your_organization_id
 * apiKey=your_api_key
 * apiUrl=https://api.ecostruxureit.com/rest/v1
 * </pre>
 */
@Disabled("Only for manual use")
@ActiveProfiles(Profiles.TEST)
@SpringBootTest
class ApiClientTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiClientTest.class);

    @Autowired
    private ApiClient apiClient;

    @MockBean
    private Configuration configuration;

    @BeforeEach
    void mockConfiguration() {

        String pathToPropertiesFileInHomeDir = System.getProperty("user.home") + "/ApiClientTest.properties";
        pathToPropertiesFileInHomeDir =
                Paths.get(pathToPropertiesFileInHomeDir).toAbsolutePath().toString();
        LOGGER.info("Trying to read configuration properties from {}", pathToPropertiesFileInHomeDir);
        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream(pathToPropertiesFileInHomeDir)) {
            properties.load(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        when(configuration.getApiKey()).thenReturn(properties.getProperty("apiKey"));
        when(configuration.getApiUrl()).thenReturn(properties.getProperty("apiUrl"));
        when(configuration.getOrganizationId()).thenReturn(properties.getProperty("organizationId"));
    }

    @Test
    void liveMeasurements() throws ApiException {

        LOGGER.info("Starting to retrieve live measurements");

        apiClient.retrieveLiveMeasurements(measurement -> LOGGER.debug("Received measurement: {}", measurement));
    }

    @Test
    void replayMeasurements() throws ApiException {

        String fromOffset = "fromOffset";
        String toOffset = "toOffset";

        LOGGER.info("Starting to replay measurements from {} to {}", fromOffset, toOffset);

        apiClient.replayMeasurements(
                fromOffset, toOffset, measurement -> LOGGER.debug("Received measurement: {}", measurement));
    }
}
