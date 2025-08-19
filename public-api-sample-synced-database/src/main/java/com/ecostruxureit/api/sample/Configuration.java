/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import jakarta.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Handles configuration of the application, e.g., API key, organization ID, and optionally API URL.
 * <p>
 * Logs if required configuration attributes have not been set.
 */
@Service
@ConfigurationProperties
public class Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    @Value("${spring.profiles.active:}")
    private Set<String> activeProfiles;

    private String apiKey;

    private String apiUrl = "https://api.ecostruxureit.com/rest/v1";

    private String organizationId;

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @PostConstruct
    void postConstruct() {

        List<String> missingConfigurationProperties = new ArrayList<>();

        if (Strings.isEmpty(apiKey)) {
            missingConfigurationProperties.add("apiKey");
        }

        if (Strings.isEmpty(apiUrl)) {
            missingConfigurationProperties.add("apiUrl");

        } else {
            try {
                new URL(apiUrl);
            } catch (MalformedURLException e) {
                String errorMessage = "Configuration parameter apiUrl contains an illegal URL: " + apiUrl;
                throw new RuntimeException(errorMessage);
            }
        }

        if (Strings.isEmpty(organizationId)) {
            missingConfigurationProperties.add("organizationId");
        }

        if (!missingConfigurationProperties.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Invalid configuration:\n\n");
            errorMessage.append("Missing configuration properties:\n");
            for (String missingConfigurationProperty : missingConfigurationProperties) {
                errorMessage.append("- ").append(missingConfigurationProperty).append("\n");
            }
            errorMessage.append("\n");
            errorMessage.append("Configuration variables can be set in several ways, for example:\n\n");

            errorMessage.append("By using command line arguments when starting the application via Maven, e.g.:\n"
                    + "./mvnw spring-boot:run -Dspring-boot.run.arguments=--apiKey=AK1/abc/xyz,--organizationId="
                    + "697bc8c3-897a-472d-abd2-0a0ebd3f703a,--apiUrl=https://api.ecostruxureit.com/rest/v1\n\n");

            errorMessage.append(
                    "By using command line arguments when starting the application as a JAR file, e.g.:\n"
                            + "java -jar public-api-sample-synced-database-1.0.0-SNAPSHOT.jar --apiKey=AK1/abc/xyz "
                            + "--organizationId=697bc8c3-897a-472d-abd2-0a0ebd3f703a --apiUrl=https://api.ecostruxureit.com/rest/v1\n\n");

            errorMessage.append(
                    "By setting properties in the application.properties file - e.g.:\n" + "apiKey=AK1/abc/xyz\n"
                            + "organizationId=697bc8c3-897a-472d-abd2-0a0ebd3f703a\n"
                            + "apiUrl=https://api.ecostruxureit.com/rest/v1\n\n");

            errorMessage.append("By setting environment variables - e.g.:\n" + "API_KEY=AK1/abc/xyz\n"
                    + "ORGANIZATION_ID=697bc8c3-897a-472d-abd2-0a0ebd3f703a\n"
                    + "API_URL=https://api.ecostruxureit.com/rest/v1\n");

            System.err.print(errorMessage);
            throw new RuntimeException(errorMessage.toString());
        }
        LOGGER.info("Will retrieve data about organization ID {} using REST URL {}", organizationId, apiUrl);
    }
}
