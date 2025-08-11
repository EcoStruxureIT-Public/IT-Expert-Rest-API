package com.ecostruxureit.api.sample;

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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Handles persistence of subtypes of {@link InventoryObject} objects in the {@code inventory_object} database table.
 * <p>
 * Also handles mapping between database {@code inventory_object} rows and Java objects - including figuring out, if a given row represents
 * a {@link Device}, a {@link Location}, or an {@link Organization} object by looking at the value of the row's discriminator column.
 */
@Repository
@Transactional(propagation = Propagation.REQUIRED)
public class InventoryObjectRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryObjectRepository.class);

    // Used to map from a row in the inventory_object table to either a Device, a Location or an Organization.
    private static final RowMapper<InventoryObject> INVENTORY_OBJECT_ROW_MAPPER = (row, rowNumber) -> {
        InventoryObject result;
        String discriminator = row.getString("discriminator");

        // Map fields specific to each of the InventoryObject subtypes

        if (Objects.equals(discriminator, Device.class.getSimpleName())) {
            Device device = new Device();
            device.setGatewayIds(convertCommaSeparatedStringToStringList(row.getString("gateway_ids")));
            device.setLocationId(row.getString("location_parent_id"));
            device.setParentId(row.getString("device_parent_id"));
            device.setType(row.getString("type"));
            device.setWarrantyExpirationDate(setDate(row.getString("warranty_expiration")));
            device.setServiceDueDate(setDate(row.getString("service_due_date")));
            device.setLastServiceDate(setDate(row.getString("last_service_date")));
            device.setDeviceNote(row.getString("device_note"));
            result = device;

        } else if (Objects.equals(discriminator, Location.class.getSimpleName())) {
            Location location = new Location();
            location.setParentId(row.getString("location_parent_id"));
            location.setType(row.getString("type"));
            location.setAddress(row.getString("address"));
            result = location;

        } else if (Objects.equals(discriminator, Organization.class.getSimpleName())) {
            Organization organization = new Organization();
            organization.setAddress(row.getString("address"));
            result = organization;

        } else {
            throw new RuntimeException("Unexpected discriminator value: " + discriminator);
        }

        // Map fields common to all the InventoryObject subtypes

        result.setId(row.getString("id"));
        result.setLabel(row.getString("label"));

        return result;
    };

    private static LocalDate setDate(String serviceDueDate) {
        if (serviceDueDate == null || serviceDueDate.isEmpty()) {
            return null;
        }
        return LocalDate.parse(serviceDueDate);
    }

    private final Configuration configuration;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    InventoryObjectRepository(
            Configuration configuration,
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {

        this.configuration = Objects.requireNonNull(configuration);
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.namedParameterJdbcTemplate = Objects.requireNonNull(namedParameterJdbcTemplate);
    }

    /**
     * Inserts the InventoryObject subtype into the inventory_object table if no row with the given ID exists - or updates the existing row
     * if one already exists.
     *
     * @param inventoryObject the Device, Location, or Organization to insert or update.
     * @throws UnexpectedNumberOfRowsAffectedException if the executed SQL did not result in exactly 1 row being inserted or changed.
     */
    void insertOrUpdate(InventoryObject inventoryObject) throws UnexpectedNumberOfRowsAffectedException {

        MapSqlParameterSource parameters = new MapSqlParameterSource();

        // Map common fields from InventoryObject
        parameters.addValue("id", inventoryObject.getId());
        parameters.addValue("discriminator", inventoryObject.getClass().getSimpleName());
        parameters.addValue("label", inventoryObject.getLabel());

        // Map fields specific to each subtype of InventoryObject

        switch (inventoryObject) {
            case Device device -> {
                parameters.addValue("type", device.getType());
                parameters.addValue("device_parent_id", device.getParentId());
                parameters.addValue("location_parent_id", device.getLocationId());
                parameters.addValue("gateway_ids", convertStringListToCommaSeparatedString(device.getGatewayIds()));
                parameters.addValue("address", null);
                parameters.addValue("warranty_expiration", device.getWarrantyExpirationDate());
                parameters.addValue("service_due_date", device.getServiceDueDate());
                parameters.addValue("last_service_date", device.getLastServiceDate());
                parameters.addValue("device_note", device.getDeviceNote());
            }
            case Location location -> {
                parameters.addValue("type", location.getType());
                parameters.addValue("device_parent_id", null);
                parameters.addValue("location_parent_id", location.getParentId());
                parameters.addValue("gateway_ids", null);
                parameters.addValue("address", location.getAddress());
                parameters.addValue("warranty_expiration", null);
                parameters.addValue("service_due_date", null);
                parameters.addValue("last_service_date", null);
                parameters.addValue("device_note", null);
            }
            case Organization organization -> {
                parameters.addValue("type", null);
                parameters.addValue("device_parent_id", null);
                parameters.addValue("location_parent_id", null);
                parameters.addValue("gateway_ids", null);
                parameters.addValue("address", organization.getAddress());
                parameters.addValue("warranty_expiration", null);
                parameters.addValue("service_due_date", null);
                parameters.addValue("last_service_date", null);
                parameters.addValue("device_note", null);
            }
            default ->
                throw new RuntimeException("Unexpected parameter type :"
                        + inventoryObject.getClass().getSimpleName());
        }

        int updatedRows = namedParameterJdbcTemplate.update(
                // Many databases have an insert-or-update statement, although they may look very different from this.
                // Alternatively one could do a delete (ignoring if fails) followed by an insert.
                "MERGE INTO inventory_object (id, discriminator, label, type, device_parent_id, location_parent_id, "
                        + "gateway_ids, address,warranty_expiration,service_due_date,last_service_date,device_note ) KEY (id)\n"
                        + "VALUES (:id, :discriminator, :label, :type, :device_parent_id, :location_parent_id, "
                        + ":gateway_ids, :address, :warranty_expiration, :service_due_date, :last_service_date, :device_note)",
                parameters);

        if (updatedRows != 1) {
            throw new UnexpectedNumberOfRowsAffectedException(1, updatedRows);
        }
    }

    /**
     * Finds the Device, Location, or Organization with the given ID in the inventory_object table.
     *
     * @param id of the InventoryObject subtype to find.
     * @return the Device, Location, or Organization with the given ID - or null if none were found.
     */
    public InventoryObject findById(String id) {

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("id", id);

        InventoryObject result;

        try {
            result = namedParameterJdbcTemplate.queryForObject(
                    "SELECT id, discriminator, label, type, device_parent_id, location_parent_id, gateway_ids, address, warranty_expiration, service_due_date, last_service_date, device_note "
                            + "FROM inventory_object "
                            + "WHERE id = :id",
                    parameters,
                    INVENTORY_OBJECT_ROW_MAPPER);
        } catch (EmptyResultDataAccessException e) {
            result = null;
        }

        LOGGER.trace(
                "{}.findById({}) -> {}",
                InventoryObjectRepository.class.getSimpleName(),
                id,
                result == null ? "null" : result.getId());
        return result;
    }

    /**
     * The inventory will contain exactly one Organization object, which contains information like name and address of the organization.
     * This method returns that Organization object.
     *
     * @return the single Organization object.
     */
    public Organization findOrganization() {
        return (Organization) findById(configuration.getOrganizationId());
    }

    /**
     * Finds the Location with the given ID in the inventory_object table.
     *
     * @param locationId the ID of the Location to find.
     * @return the Location with the given ID - or null if no Location with the given ID was found.
     */
    public Location findLocationById(String locationId) {
        InventoryObject inventoryObject = findById(locationId);
        if (inventoryObject instanceof Location location) {
            return location;
        }
        return null;
    }

    /**
     * Finds all the Devices, Locations, and Organizations in the inventory_object table.
     *
     * @return all objects that are subtypes of InventoryObject.
     */
    List<InventoryObject> findAll() {
        return jdbcTemplate.query(
                "SELECT id, discriminator, label, type, device_parent_id, location_parent_id, gateway_ids, address, warranty_expiration, service_due_date, last_service_date, device_note FROM inventory_object",
                INVENTORY_OBJECT_ROW_MAPPER);
    }

    /**
     * Return a list of rows. A row will be represented by a map, where key is the column name and value is the cell value. E.g.:
     * <pre>
     *     list-index-0 contains a map with [id=0, discriminator='Device',   label='UPS 24', ...]
     *     list-index-1 contains a map with [id=1, discriminator='Location', label='Rack 2', ...]
     *     ...
     * </pre>
     *
     * @return all the Devices, Locations, and Organizations from the inventory_object table
     */
    public List<Map<String, Object>> findAllAsListOfMapsOrderedByDiscriminatorDesc() {
        return jdbcTemplate.queryForList(
                "SELECT id, discriminator, label, type, device_parent_id, location_parent_id, gateway_ids, address, warranty_expiration, service_due_date, last_service_date, device_note "
                        + "FROM inventory_object ORDER BY discriminator DESC");
    }

    /**
     * Finds all the inventory objects (Devices, Locations, and Organization) in the inventory_object table, and return their IDs.
     *
     * @return the IDs of all the InventoryObject subtypes.
     */
    Set<String> findAllIds() {
        return new HashSet<>(jdbcTemplate.queryForList("SELECT id FROM inventory_object", String.class));
    }

    /**
     * Finds the Devices that are the parent of this device (or null if it doesn't have a parent). E.g. given the devices below, then:
     * <ul>
     *     <li>findParentDevice(device-a) would return null
     *     <li>findParentDevice(device-b) would return device-a
     *     <li>findParentDevice(device-d) would return device-c
     * </ul>
     * <pre>
     *           device-a
     *              |
     *      +-------+-------+
     *      |               |
     *   device-b        device-c
     *                      |
     *                   device-d
     * </pre>
     *
     * @param device the Device whose parent to find
     * @return the immediate parent of the given device, or null if the device does not have a parent device
     */
    Device findParentDevice(Device device) {
        Device result;
        String deviceParentId = device.getParentId();
        if (deviceParentId == null) {
            result = null;
        } else {
            InventoryObject parent = findById(deviceParentId);
            if (!(parent instanceof Device parentDevice)) {
                // This can happen because the Public API returns eventually consistent data.
                LOGGER.debug(
                        "Device {} referenced a device parent ID {}, which doesn't exist or wasn't a Device",
                        extractDeviceId(device),
                        extractDeviceParentId(device));
                result = null;
            } else {
                result = parentDevice;
            }
        }

        LOGGER.trace(
                "{}.findParentDevice({}) -> {}",
                getClass().getSimpleName(),
                extractDeviceId(device),
                extractDeviceId(result));
        return result;
    }

    /**
     * Finds the Devices that is the root in the device-tree in which the given device is a part of. May return the device itself, if it is
     * the root in the given device-tree. E.g. given the devices below, then:
     * <ul>
     *     <li>findRootDevice(device-a) would return device-a
     *     <li>findRootDevice(device-b) would return device-a
     *     <li>findRootDevice(device-d) would return device-a
     * </ul>
     * <pre>
     *           device-a
     *              |
     *      +-------+-------+
     *      |               |
     *   device-b        device-c
     *                      |
     *                   device-d
     * </pre>
     *
     * @param device the Device which is part of a device-tree whose root we want to find
     * @return the root of the device-tree
     */
    public Device findRootDevice(Device device) {

        Device currentDevice = device;

        int iterations = 0;

        while (true) {
            iterations++;
            if (iterations > 100) {
                // Safe-guard to not end in never ending loop in case of invalid data
                throw new RuntimeException("After 100 iterations have still not reached root device");
            }
            Device parentDevice = findParentDevice(currentDevice);
            if (parentDevice == null) {
                break;
            }
            currentDevice = parentDevice;
        }

        LOGGER.trace(
                "{}.findRootDevice({}) -> {}",
                getClass().getSimpleName(),
                extractDeviceId(device),
                extractDeviceId(currentDevice));
        return currentDevice;
    }

    /**
     * Finds all the Devices that have the given device as their parent. E.g. given the devices below, then:
     * <ul>
     *     <li>findAllChildDevices(device-a) would return a list with device-b and device-c
     *     <li>findAllChildDevices(device-b) would return an empty list
     *     <li>findAllChildDevices(device-c) would return a list with device-d
     * </ul>
     * <pre>
     *           device-a
     *              |
     *      +-------+-------+
     *      |               |
     *   device-b        device-c
     *                      |
     *                   device-d
     * </pre>
     *
     * @param device the Device whose children to find
     * @return the immediate children of the given device, which will be an empty list, if the device does not have any children
     */
    public List<Device> findAllChildDevices(Device device) {

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("device_parent_id", device.getId());

        List<InventoryObject> resultAsInventoryObjects = namedParameterJdbcTemplate.query(
                "SELECT id, discriminator, label, type, device_parent_id, location_parent_id, gateway_ids, address, warranty_expiration, service_due_date, last_service_date, device_note "
                        + "FROM inventory_object "
                        + "WHERE device_parent_id = :device_parent_id",
                parameters,
                INVENTORY_OBJECT_ROW_MAPPER);

        List<Device> resultAsDevices = resultAsInventoryObjects.stream()
                .filter(inventoryObject -> inventoryObject instanceof Device) // just to guard against invalid data
                .map(inventoryObject -> (Device) inventoryObject)
                .collect(Collectors.toList());

        LOGGER.trace(
                "{}.findAllChildDevices({}) -> {}",
                getClass().getSimpleName(),
                device.getId(),
                resultAsDevices.stream().map(InventoryObject::getId).collect(Collectors.toList()));

        return resultAsDevices;
    }

    /**
     * Deletes the InventoryObject subtype objects with the given ID from the inventory_object table. Silently ignores if trying to delete a
     * non-existing ID.
     *
     * @param id the IDs of the object to delete.
     */
    public void deleteById(String id) {
        deleteByIds(Collections.singleton(id));
    }

    /**
     * Deletes all the InventoryObject subtype objects from the inventory_object table, that have one of the given IDs. Silently ignores if
     * trying to delete a non-existing ID.
     *
     * @param ids the IDs of the objects to delete.
     */
    public void deleteByIds(Set<String> ids) {

        // The JDBC standard does not guarantee that you can use more than 100 values for an IN expression list.
        // Various databases exceed this number, but they usually have a hard limit for how many values are allowed.
        // See https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#jdbc-in-clause
        // Therefore, we delete the ids in chunks of no more than 100 per chunk.

        Set<String> max100ids = Sets.newHashSetWithExpectedSize(100);
        for (String id : ids) {
            max100ids.add(id);
            if (max100ids.size() == 100) {
                deleteByIdsMax100(max100ids);
                max100ids.clear();
            }
        }
        if (max100ids.size() > 0) {
            deleteByIdsMax100(max100ids);
        }
    }

    private void deleteByIdsMax100(Set<String> ids) {

        if (ids.size() > 100) {
            throw new IllegalArgumentException(
                    "Parameter ids must not contain more than 100 entries, but contained " + ids.size());
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", ids);

        namedParameterJdbcTemplate.update("DELETE FROM inventory_object WHERE id IN (:ids)", parameters);
    }

    private static String convertStringListToCommaSeparatedString(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return null;
        }
        return String.join(",", strings);
    }

    private static List<String> convertCommaSeparatedStringToStringList(String commaSeparatedStrings) {
        if (commaSeparatedStrings == null) {
            return null;
        }
        return Arrays.asList(commaSeparatedStrings.split(","));
    }

    private String extractDeviceId(Device device) {
        if (device == null) {
            return "null";
        } else {
            return device.getId();
        }
    }

    private String extractDeviceParentId(Device device) {
        if (device == null) {
            return "null";
        } else {
            return device.getParentId();
        }
    }
}
