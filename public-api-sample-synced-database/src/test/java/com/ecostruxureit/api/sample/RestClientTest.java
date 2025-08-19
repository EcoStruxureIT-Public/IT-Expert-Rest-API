/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import generated.dto.AlarmChangesResponse;
import generated.dto.AlarmsResponse;
import generated.dto.InventoryChangesResponse;
import generated.dto.InventoryResponse;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Makes requests to the real REST API, so turned off via the Disabled annotation to prevent from being run during builds of the project.
 * <p>
 * To use you must create a file in your home directory called {@code RestClientTest.properties} with the following content:
 * <pre>
 * organizationId=your_organization_id
 * apiKey=your_api_key
 * apiUrl=https://api.ecostruxureit.com/rest/v1
 * </pre>
 */
@Disabled("Only for manual use")
@ActiveProfiles(Profiles.TEST)
@SpringBootTest
class RestClientTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientTest.class);

    @Autowired
    private RestClient restClient;

    @MockBean
    private Configuration configuration;

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

        Mockito.when(configuration.getApiKey()).thenReturn(properties.getProperty("apiKey"));
        Mockito.when(configuration.getApiUrl()).thenReturn(properties.getProperty("apiUrl"));
        Mockito.when(configuration.getOrganizationId()).thenReturn(properties.getProperty("organizationId"));
    }

    @Test
    void listInventory() {
        InventoryResponse response = restClient.listInventory();

        System.out.println("======================================");
        System.out.println("Called RestClient.listInventory()");
        System.out.println("Response.getOffset(): " + response.getOffset());
        System.out.println("Response.getInventoryObjects().size(): "
                + response.getInventoryObjects().size());
        // System.out.println("Full response: " + response);
        System.out.println("======================================");
    }

    @Test
    void getInventoryChangesAfterOffset() {
        long currentOffset = 0L;
        int callNumber = 0;
        // Keep fetching changes until we have fetched them all, which is when the offset stays unchanged across calls,
        // which may happen in
        // just a single API call, depending on the amount of changes.
        while (true) {

            callNumber++;

            InventoryChangesResponse response = restClient.getInventoryChangesAfterOffset(currentOffset);

            System.out.println("======================================");
            System.out.println("Made call number " + callNumber + " to RestClient.getInventoryChangesAfterOffset("
                    + currentOffset + ")");
            System.out.println("Response.getOffset() : " + response.getOffset());
            System.out.println("Response.getInventoryObjectChanges().size() : "
                    + response.getInventoryObjectChanges().size());
            // System.out.println("Full response: " + response);
            System.out.println("======================================");

            if (currentOffset == response.getOffset()) {
                break;
            }
            currentOffset = response.getOffset();
        }
    }

    @Test
    void listAlarms() {
        AlarmsResponse response = restClient.listAlarms();

        System.out.println("======================================");
        System.out.println("Called RestClient.listAlarms()");
        System.out.println("Response.getOffset(): " + response.getOffset());
        System.out.println(
                "Response.getAlarms().size(): " + response.getAlarms().size());
        // System.out.println("Full response: " + response);
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
