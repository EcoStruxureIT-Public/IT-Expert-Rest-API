package com.ecostruxureit.api.sample;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import generated.dto.Measurement;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
@SpringBootTest
@ActiveProfiles(Profiles.TEST)
class SwaggerMeasurementTest {

    @Value("classpath:/measurement.json")
    private Resource measurementResource;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void measurement() throws IOException {

        Measurement measurement = objectMapper.readValue(measurementResource.getURL(), Measurement.class);

        assertThat(measurement).isNotNull();
    }
}
