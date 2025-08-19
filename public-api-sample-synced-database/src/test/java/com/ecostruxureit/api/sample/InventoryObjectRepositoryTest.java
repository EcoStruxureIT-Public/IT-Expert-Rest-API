/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import generated.dto.Device;
import generated.dto.InventoryObject;
import generated.dto.Location;
import generated.dto.Organization;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the methods in {@link InventoryObjectRepository}.
 * <p>
 * Using the {@link Transactional} annotation on a test class means that test methods will each be run in a separate transaction, which is
 * rolled back, when the test finishes - which means test methods won't "pollute" the database with data that may make other tests fail.
 */
@SpringBootTest
@ActiveProfiles(Profiles.TEST)
@Transactional
class InventoryObjectRepositoryTest {

    @Autowired
    private Configuration configuration;

    @Autowired
    private InventoryObjectRepository inventoryObjectRepository;

    @Autowired
    private TestData testData;

    @Test
    void insertAndLoadOrganization() {

        String organizationId = configuration.getOrganizationId();

        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setLabel("Flamingo Inc.");
        organization.setAddress("Silcon Alle 1, 6000 Kolding, Denmark");

        inventoryObjectRepository.insertOrUpdate(organization);

        assertEquals(1, inventoryObjectRepository.findAll().size());

        Organization organizationFromRepository = (Organization) inventoryObjectRepository.findById(organizationId);
        assertEquals(organizationId, organizationFromRepository.getId());
        assertEquals("Flamingo Inc.", organizationFromRepository.getLabel());
        assertEquals("Silcon Alle 1, 6000 Kolding, Denmark", organizationFromRepository.getAddress());
    }

    @Test
    void insertAndLoadLocation() {

        Location location = new Location();
        location.setId("loc-id");
        location.setLabel("Kolding");
        location.setType("SITE");
        location.setParentId("loc-parent-id");
        location.setAddress("Somewhere nice");

        inventoryObjectRepository.insertOrUpdate(location);

        assertEquals(1, inventoryObjectRepository.findAll().size());

        Location locationFromRepository = (Location) inventoryObjectRepository.findById("loc-id");
        assertEquals("loc-id", locationFromRepository.getId());
        assertEquals("Kolding", locationFromRepository.getLabel());
        assertEquals("SITE", locationFromRepository.getType());
        assertEquals("loc-parent-id", locationFromRepository.getParentId());
        assertEquals("Somewhere nice", locationFromRepository.getAddress());
    }

    @Test
    void insertAndLoadDevice() {

        Device device = new Device();
        device.setId("device-id");
        device.setLabel("UPS 12");
        device.setType("RPDU");
        device.setGatewayIds(Arrays.asList("gw1", "gw2"));
        device.setLocationId("device-location-id");
        device.setParentId("device-parent-id");

        inventoryObjectRepository.insertOrUpdate(device);

        assertEquals(1, inventoryObjectRepository.findAll().size());

        Device deviceFromRepository = (Device) inventoryObjectRepository.findById("device-id");
        assertEquals("device-id", deviceFromRepository.getId());
        assertEquals("UPS 12", deviceFromRepository.getLabel());
        assertEquals("RPDU", deviceFromRepository.getType());
        assertEquals(Arrays.asList("gw1", "gw2"), deviceFromRepository.getGatewayIds());
        assertEquals("device-location-id", deviceFromRepository.getLocationId());
        assertEquals("device-parent-id", deviceFromRepository.getParentId());
    }

    @Test
    void insertInvalidType() {

        InventoryObject inventoryObject = new InventoryObject();
        inventoryObject.setId("some-id");

        // Nothing is just an InventoryObject - it is always and Organization, Device, or Location, but the
        // InventoryObject generated based
        // on the Open API Specification is not made abstract.
        assertThrows(RuntimeException.class, () -> inventoryObjectRepository.insertOrUpdate(inventoryObject));
    }

    @Test
    void findById_whenIdDoesNotExist_thenReturnsNull() {
        assertNull(inventoryObjectRepository.findById("does_not_exist"));
    }

    @Test
    void findOrganization_whenOrganizationExist_thenReturnsOrganization() {
        Organization organization = new Organization();
        organization.setId(configuration.getOrganizationId());

        inventoryObjectRepository.insertOrUpdate(organization);

        assertEquals(organization, inventoryObjectRepository.findOrganization());
    }

    @Test
    void findOrganization_whenNoOrganizationExist_thenReturnsNull() {
        assertNull(inventoryObjectRepository.findOrganization());
    }

    @Test
    void findLocationById_whenOrganizationExist_thenReturnsOrganization() {
        Location location = new Location();
        location.setId("loc-id");

        inventoryObjectRepository.insertOrUpdate(location);

        assertEquals(location, inventoryObjectRepository.findLocationById("loc-id"));
    }

    @Test
    void findLocationById_whenLocationDoesNotExist_thenReturnsNull() {
        assertNull(inventoryObjectRepository.findLocationById("non-existing-id"));
    }

    @Test
    void findAll() {

        String organizationId = configuration.getOrganizationId();

        Organization organization = new Organization();
        organization.setId(organizationId);
        inventoryObjectRepository.insertOrUpdate(organization);

        Location location = new Location();
        location.setId("Location-ID");
        inventoryObjectRepository.insertOrUpdate(location);

        Device device = new Device();
        device.setId("Device-ID");
        device.setGatewayIds(Collections.singletonList("Gateway-ID"));
        inventoryObjectRepository.insertOrUpdate(device);

        List<InventoryObject> inventoryObjects = inventoryObjectRepository.findAll();

        assertEquals(3, inventoryObjects.size());
        assertTrue(inventoryObjects.contains(organization));
        assertTrue(inventoryObjects.contains(location));
        assertTrue(inventoryObjects.contains(device));
    }

    @Test
    void findAllAsListOfMapsOrderedByDiscriminatorDesc() {

        Organization organization = new Organization();
        organization.setId("Organization-ID");
        inventoryObjectRepository.insertOrUpdate(organization);

        Location location = new Location();
        location.setId("Location-ID");
        inventoryObjectRepository.insertOrUpdate(location);

        Device device = new Device();
        device.setId("Device-ID");
        inventoryObjectRepository.insertOrUpdate(device);

        List<Map<String, Object>> inventoryObjects =
                inventoryObjectRepository.findAllAsListOfMapsOrderedByDiscriminatorDesc();

        assertEquals(3, inventoryObjects.size());

        // Should be sorted desc by discriminator - so the Organization should be first, then the Locations, and finally
        // the Devices.
        assertEquals("Organization-ID", inventoryObjects.get(0).get("id"));
        assertEquals("Location-ID", inventoryObjects.get(1).get("id"));
        assertEquals("Device-ID", inventoryObjects.get(2).get("id"));
    }

    @Test
    void findAllIds() {

        Organization organization = new Organization();
        organization.setId("2dbdd0a8-7565-4da4-98fd-0879b381e02f");
        organization.setLabel("Flamingo Inc.");

        Location location = new Location();
        location.setId("ddb94d37-b2c0-4e4f-8500-1c598a5494ee");
        location.setLabel("Kolding");
        location.setType("SITE");

        Device device = new Device();
        device.setId("43bf47cf-48ce-413a-80ea-339890a830a0");
        device.setLabel("UPS 12");

        inventoryObjectRepository.insertOrUpdate(organization);
        inventoryObjectRepository.insertOrUpdate(location);
        inventoryObjectRepository.insertOrUpdate(device);

        Set<String> allIds = inventoryObjectRepository.findAllIds();

        assertEquals(3, allIds.size());
        assertTrue(allIds.contains(organization.getId()));
        assertTrue(allIds.contains(location.getId()));
        assertTrue(allIds.contains(device.getId()));
    }

    @Test
    void findParentDevice() {

        testData.insertTestDataIntoDatabase();

        Device device_d = (Device) inventoryObjectRepository.findById("device_d");
        Device device_h = (Device) inventoryObjectRepository.findById("device_h");

        assertNull(inventoryObjectRepository.findParentDevice(device_d));
        assertEquals(device_d, inventoryObjectRepository.findParentDevice(device_h));
    }

    @Test
    void findRootDevice() {

        testData.insertTestDataIntoDatabase();

        Device device_d = (Device) inventoryObjectRepository.findById("device_d");
        Device device_h = (Device) inventoryObjectRepository.findById("device_h");
        Device device_i = (Device) inventoryObjectRepository.findById("device_i");

        assertEquals(device_d, inventoryObjectRepository.findRootDevice(device_d));
        assertEquals(device_d, inventoryObjectRepository.findRootDevice(device_h));
        assertEquals(device_d, inventoryObjectRepository.findRootDevice(device_i));
    }

    @Test
    void deviceHasWarrantyAndServiceInformation() {
        testData.insertTestDataIntoDatabase();

        Device device_d = (Device) inventoryObjectRepository.findById("device_d");

        Assertions.assertThat(device_d)
                .extracting("warrantyExpirationDate", "serviceDueDate", "lastServiceDate", "deviceNote")
                .contains(
                        LocalDate.parse("2025-03-31"),
                        LocalDate.parse("2025-03-31"),
                        LocalDate.parse("2025-03-31"),
                        "Device note D");
    }

    @Test
    void findAllChildDevices() {

        testData.insertTestDataIntoDatabase();

        Device device_d = (Device) inventoryObjectRepository.findById("device_d");
        Device device_g = (Device) inventoryObjectRepository.findById("device_g");
        Device device_h = (Device) inventoryObjectRepository.findById("device_h");
        Device device_i = (Device) inventoryObjectRepository.findById("device_i");

        List<Device> childrenOfD = inventoryObjectRepository.findAllChildDevices(device_d);
        assertEquals(2, childrenOfD.size());
        assertTrue(childrenOfD.contains(device_g));
        assertTrue(childrenOfD.contains(device_h));

        List<Device> childrenOfH = inventoryObjectRepository.findAllChildDevices(device_h);
        assertEquals(1, childrenOfH.size());
        assertTrue(childrenOfH.contains(device_i));

        List<Device> childrenOfI = inventoryObjectRepository.findAllChildDevices(device_i);
        assertEquals(0, childrenOfI.size());
    }

    @Test
    void deleteById() {

        String organizationId = configuration.getOrganizationId();
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setLabel("Flamingo Inc.");

        String locationId = "6ad4a6e5-d98e-4ea2-bfb0-f1ea11f5c49f";
        Location location = new Location();
        location.setId(locationId);
        location.setLabel("Kolding");
        location.setType("SITE");

        inventoryObjectRepository.insertOrUpdate(organization);
        inventoryObjectRepository.insertOrUpdate(location);

        inventoryObjectRepository.deleteById(locationId);

        List<InventoryObject> allInventoryObjects = inventoryObjectRepository.findAll();

        assertEquals(1, allInventoryObjects.size());
        assertEquals("Flamingo Inc.", allInventoryObjects.getFirst().getLabel());
    }

    @Test
    void deleteById_whenIdDoesNotExist_thenSilentlyIgnores() {

        inventoryObjectRepository.deleteById("non_existing_id");
    }

    @Test
    void deleteByIds() {

        String organizationId = configuration.getOrganizationId();
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setLabel("Flamingo Inc.");

        Location location = new Location();
        location.setId("6ad4a6e5-d98e-4ea2-bfb0-f1ea11f5c49f");
        location.setLabel("Kolding");
        location.setType("SITE");

        Device device = new Device();
        device.setId("18707764-063e-4e7c-a5d9-37d76566c8dc");
        device.setLabel("UPS 12");

        inventoryObjectRepository.insertOrUpdate(organization);
        inventoryObjectRepository.insertOrUpdate(location);
        inventoryObjectRepository.insertOrUpdate(device);

        Set<String> idsToDelete = new HashSet<>();
        idsToDelete.add(organization.getId());
        idsToDelete.add(device.getId());

        inventoryObjectRepository.deleteByIds(idsToDelete);

        List<InventoryObject> allInventoryObjects = inventoryObjectRepository.findAll();

        assertEquals(1, allInventoryObjects.size());
        assertEquals("Kolding", allInventoryObjects.getFirst().getLabel());
    }

    @Test
    void deleteByIds_whenMoreThan100Ids_thenStillWorks() {

        Device donNotDeleteMe = new Device();
        donNotDeleteMe.setId("18707764-063e-4e7c-a5d9-37d76566c8dc");
        inventoryObjectRepository.insertOrUpdate(donNotDeleteMe);

        int entriesToKeep = 1;
        int entriesToDelete = 120;

        Set<String> idsToDelete = Sets.newHashSetWithExpectedSize(entriesToDelete);

        for (int n = 0; n < entriesToDelete; n++) {
            String id = ("id-" + n);
            idsToDelete.add(id);
            Device deviceToDelete = new Device();
            deviceToDelete.setId(id);
            inventoryObjectRepository.insertOrUpdate(deviceToDelete);
        }

        assertEquals(
                entriesToDelete + entriesToKeep,
                inventoryObjectRepository.findAll().size());

        inventoryObjectRepository.deleteByIds(idsToDelete);

        assertEquals(entriesToKeep, inventoryObjectRepository.findAll().size());
    }

    @Test
    void deleteByIds_whenNotAllIdsExist_thenSilentlyIgnores() {

        String organizationId = configuration.getOrganizationId();
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setLabel("Flamingo Inc.");

        inventoryObjectRepository.insertOrUpdate(organization);

        Set<String> idsToDelete = new HashSet<>();
        idsToDelete.add(organization.getId());
        idsToDelete.add("non_existing_id");

        inventoryObjectRepository.deleteByIds(idsToDelete);
    }
}
