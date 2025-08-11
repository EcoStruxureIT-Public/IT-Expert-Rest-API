package com.ecostruxureit.api.sample.webui;

import static com.ecostruxureit.api.sample.webui.ControllerAssertions.assertContainsSubstring;
import static com.ecostruxureit.api.sample.webui.ControllerAssertions.assertContentInFirstPreElement;
import static com.ecostruxureit.api.sample.webui.ControllerAssertions.assertOccurrencesOfSubstring;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ecostruxureit.api.sample.Profiles;
import com.ecostruxureit.api.sample.PublicApiSampleClientApplication;
import com.ecostruxureit.api.sample.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 */
@SpringBootTest(classes = PublicApiSampleClientApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.TEST)
class InventoryControllerTest {

    // Meant for testing REST endpoints - but works fine for retrieving HTML pages as well
    @Autowired
    private TestRestTemplate httpClient;

    @Autowired
    private TestData testData;

    /**
     * In this test we cannot use @Transactional as we do in some other tests, as the data inserted in a transaction won't be visible to
     * when accessing the data via the controller (as it will run in its own separate transaction).
     */
    @AfterEach
    void cleanupDatabase() {
        testData.resetDatabase();
    }

    @Test
    void inventoryObjects_whenEmpty_thenTableWithSingleNoInventoryObjectsRowIsShown() {

        // Don't insert any data

        ResponseEntity<String> htmlResponse =
                httpClient.getForEntity(InventoryObjectController.INVENTORY_OBJECTS_PATH, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        int expectedTrElementCount = 2; // 1 for header and 1 for "No alarms" row
        assertOccurrencesOfSubstring(html, "<tr>", expectedTrElementCount);
        assertContainsSubstring(html, "No inventory objects");
    }

    @Test
    void inventoryObjects_whenSomeInventoryObjects_thenRowPerInventoryObjectIsShown() {

        testData.insertTestDataIntoDatabase(); // Inserts 9 inventory objects

        ResponseEntity<String> htmlResponse =
                httpClient.getForEntity(InventoryObjectController.INVENTORY_OBJECTS_PATH, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        int expectedTrElementCount = 10; // 1 for header and 1 per inventory object
        assertOccurrencesOfSubstring(html, "<tr>", expectedTrElementCount);
        assertContainsSubstring(html, "organization_a");
        assertContainsSubstring(html, "location_b");
        assertContainsSubstring(html, "device_d");
    }

    // Should give the same result no matter which one of the devices in "composite" device we request
    @ParameterizedTest
    @ValueSource(strings = {"device_d", "device_g", "device_h", "device_i"})
    void inventoryObject_whenRequestingDeviceWhichIsPartOfCompositeDeviceWhereSomeContainAlarms(String deviceId) {

        testData.insertTestDataIntoDatabase();

        String path = InventoryObjectController.INVENTORY_OBJECTS_PATH + "/" + deviceId;
        ResponseEntity<String> htmlResponse = httpClient.getForEntity(path, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        String expectedLineStarts = ""
                + "Organization\n"
                + "   Location\n"
                + "      Location\n"
                + "         Device\n"
                + "            * Alarm\n"
                + "            Device\n"
                + "            Device\n"
                + "               * Alarm\n"
                + "               Device\n"
                + "                  * Alarm\n"
                + "                  * Alarm";

        assertContentInFirstPreElement(html, expectedLineStarts);
    }

    @Test
    void inventoryObject_whenRequestingStandaloneDeviceWithLocation() {

        testData.insertTestDataIntoDatabase();

        String path = InventoryObjectController.INVENTORY_OBJECTS_PATH + "/device_e";
        ResponseEntity<String> htmlResponse = httpClient.getForEntity(path, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        String expectedLineStarts = "" + "Organization\n" + "   Location\n" + "      Device";

        assertContentInFirstPreElement(html, expectedLineStarts);
    }

    @Test
    void inventoryObject_whenRequestingStandaloneDeviceWithoutLocation() {

        testData.insertTestDataIntoDatabase();

        String path = InventoryObjectController.INVENTORY_OBJECTS_PATH + "/device_f";
        ResponseEntity<String> htmlResponse = httpClient.getForEntity(path, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        String expectedLineStarts = "" + "Organization\n" + "   Device";

        assertContentInFirstPreElement(html, expectedLineStarts);
    }

    @Test
    void inventoryObject_whenRequestingLocationWithOneParent() {

        testData.insertTestDataIntoDatabase();

        String path = InventoryObjectController.INVENTORY_OBJECTS_PATH + "/location_c";
        ResponseEntity<String> htmlResponse = httpClient.getForEntity(path, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        String expectedLineStarts = "" + "Organization\n" + "   Location\n" + "      Location";

        assertContentInFirstPreElement(html, expectedLineStarts);
    }

    @Test
    void inventoryObject_whenRequestingLocationWithoutParent() {

        testData.insertTestDataIntoDatabase();

        String path = InventoryObjectController.INVENTORY_OBJECTS_PATH + "/location_b";
        ResponseEntity<String> htmlResponse = httpClient.getForEntity(path, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        String expectedLineStarts = "" + "Organization\n" + "   Location";

        assertContentInFirstPreElement(html, expectedLineStarts);
    }

    @Test
    void inventoryObject_whenRequestingOrganization() {

        testData.insertTestDataIntoDatabase();

        String path = InventoryObjectController.INVENTORY_OBJECTS_PATH + "/organization_a";
        ResponseEntity<String> htmlResponse = httpClient.getForEntity(path, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        String expectedLineStarts = "" + "Organization";

        assertContentInFirstPreElement(html, expectedLineStarts);
    }
}
