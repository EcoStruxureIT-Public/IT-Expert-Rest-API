package com.ecostruxureit.api.sample;

import generated.dto.AlarmChangesResponse;
import generated.dto.GetDeviceResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Makes requests to the real REST API, so turned off via the Disabled annotation to prevent from being run during builds of the project.
 * <p>
 * To use you must create a file in your home directory called {@code RestClientTest.properties} with the following content:
 * <pre>
 * organizationId=your_organization_id
 * apiKey=your_api_key
 * apiUrl=https://api.ecostruxureit.com/rest/v1
 * deviceIdToFetch=your_device_id
 * </pre>
 */
@Disabled("Only for manual use")
@ActiveProfiles(Profiles.TEST)
@SpringBootTest
class RestClientTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientTest.class);

    @Autowired
    private RestClient restClient;

    @MockitoBean
    private Configuration configuration;

    private String deviceIdToFetch;

    @BeforeEach
    void mockConfiguration() {
        String pathToPropertiesFileInHomeDir = System.getProperty("user.home") + "/RestClientTest.properties";
        pathToPropertiesFileInHomeDir =
                Paths.get(pathToPropertiesFileInHomeDir).toAbsolutePath().toString();
        LOGGER.info("Trying to read configuration properties from {}", pathToPropertiesFileInHomeDir);
        Properties properties = new Properties();

        try (InputStream is = new FileInputStream(pathToPropertiesFileInHomeDir)) {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        deviceIdToFetch = properties.getProperty("deviceIdToFetch");

        Mockito.when(configuration.getApiKey()).thenReturn(properties.getProperty("apiKey"));
        Mockito.when(configuration.getApiUrl()).thenReturn(properties.getProperty("apiUrl"));
        Mockito.when(configuration.getOrganizationId()).thenReturn(properties.getProperty("organizationId"));
    }

    @Test
    void getDevice() {
        LOGGER.info("Will try to fetch Device with ID: {}", deviceIdToFetch);
        GetDeviceResponse response = restClient.getDevice(deviceIdToFetch);

        System.out.println("======================================");
        System.out.println("Called RestClient.getDevice(" + deviceIdToFetch + ")");
        System.out.println("Full response: " + response);
        System.out.println("======================================");
    }

    @Test
    void getAlarmChangesAfterOffset() {
        long currentOffset = 0L;
        int callNumber = 0;
        // Keep fetching changes until we have fetched them all, which is when the offset stays unchanged across calls,
        // which may happen in
        // just a single API call, depending on the amount of changes.
        while (true) {

            callNumber++;

            AlarmChangesResponse response = restClient.getAlarmChangesAfterOffset(currentOffset);

            System.out.println("======================================");
            System.out.println("Made call number " + callNumber + " to RestClient.getAlarmChangesAfterOffset("
                    + currentOffset + ")");
            System.out.println("Response.getOffset() : " + response.getOffset());
            System.out.println(
                    "Response.getAlarms().size() : " + response.getAlarms().size());
            // System.out.println("Full response: " + response);
            System.out.println("======================================");

            if (currentOffset == response.getOffset()) {
                break;
            }
            currentOffset = response.getOffset();
        }
    }
}
