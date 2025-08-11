package com.ecostruxureit.api.sample.webui;

import static com.ecostruxureit.api.sample.webui.ControllerAssertions.assertContainsSubstring;
import static com.ecostruxureit.api.sample.webui.ControllerAssertions.assertOccurrencesOfSubstring;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ecostruxureit.api.sample.Profiles;
import com.ecostruxureit.api.sample.PublicApiSampleClientApplication;
import com.ecostruxureit.api.sample.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
class AlarmControllerTest {

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
    void alarms_whenNoAlarms_thenTableWithSingleNoAlarmsRowIsShown() {

        // Don't insert any data

        ResponseEntity<String> htmlResponse = httpClient.getForEntity(AlarmController.ALARMS_PATH, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        int expectedTrElementCount = 2; // 1 for header and 1 for "No alarms" row
        assertOccurrencesOfSubstring(html, "<tr>", expectedTrElementCount);
        assertContainsSubstring(html, "No alarms");
    }

    @Test
    void alarms_whenSomeAlarms_thenRowPerAlarmIsShown() {

        testData.insertTestDataIntoDatabase(); // Inserts 4 alarms

        ResponseEntity<String> htmlResponse = httpClient.getForEntity(AlarmController.ALARMS_PATH, String.class);
        assertEquals(HttpStatus.OK, htmlResponse.getStatusCode());
        String html = htmlResponse.getBody();

        int expectedTrElementCount = 5; // 1 for header and 1 per alarm
        assertOccurrencesOfSubstring(html, "<tr>", expectedTrElementCount);
        assertContainsSubstring(html, "alarm_d1");
        assertContainsSubstring(html, "alarm_h1");
        assertContainsSubstring(html, "alarm_i1");
        assertContainsSubstring(html, "alarm_i2");
    }
}
