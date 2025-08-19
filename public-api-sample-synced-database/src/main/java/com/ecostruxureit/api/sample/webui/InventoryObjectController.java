/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.webui;

import com.ecostruxureit.api.sample.AlarmRepository;
import com.ecostruxureit.api.sample.InventoryObjectRepository;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import generated.dto.Alarm;
import generated.dto.Device;
import generated.dto.InventoryObject;
import generated.dto.Location;
import generated.dto.Organization;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Handles the page that renders all inventory items stored in the database.
 * <p>
 * Also handles the page that shows a detailed tree in which a given inventory object is a part.
 */
@SuppressWarnings("UnstableApiUsage")
@Controller
public class InventoryObjectController {

    static final String INVENTORY_OBJECTS_PATH = "/inventory-objects";

    private final AlarmRepository alarmRepository;
    private final InventoryObjectRepository inventoryObjectRepository;

    public InventoryObjectController(
            AlarmRepository alarmRepository, InventoryObjectRepository inventoryObjectRepository) {

        this.alarmRepository = Objects.requireNonNull(alarmRepository);
        this.inventoryObjectRepository = Objects.requireNonNull(inventoryObjectRepository);
    }

    @GetMapping(INVENTORY_OBJECTS_PATH)
    public String inventoryObjects(Model model) {

        List<Map<String, Object>> rows = inventoryObjectRepository.findAllAsListOfMapsOrderedByDiscriminatorDesc();
        model.addAttribute("rows", rows);

        // Rendered by the template by the same name + html suffix (the model is made available to the template)
        return "inventory-object-list";
    }

    @GetMapping(INVENTORY_OBJECTS_PATH + "/{inventoryObjectId}")
    public String inventoryObject(Model model, @PathVariable("inventoryObjectId") String inventoryObjectId) {

        // MutableGraph can be used to represent a tree, but easiest if you "remember" the root node of the tree
        // yourself (as we do below)
        MutableGraph<Object> tree =
                GraphBuilder.directed().allowsSelfLoops(false).build();
        InventoryObject treeRoot = null;

        InventoryObject inventoryObject = inventoryObjectRepository.findById(inventoryObjectId);

        if (inventoryObject instanceof Device device) {
            Device rootDevice = inventoryObjectRepository.findRootDevice(device);
            insertDeviceAndItsDescendantDevicesIntoTree(tree, rootDevice);
            insertAlarmsIntoTree(tree);
            treeRoot = insertLocationAncestorsIntoTreeAndReturnRoot(tree, rootDevice);

        } else if (inventoryObject instanceof Location) {

            // If the requested inventory object is a location, then we just want the location ancestors of that
            // location.
            treeRoot = insertLocationAncestorsIntoTreeAndReturnRoot(tree, inventoryObject);
        }

        // Finally always insert the organization object as the root of the tree.
        treeRoot = insertOrganizationIntoTreeAndReturnRoot(tree, treeRoot);

        // Write the tree as raw text that is included as is in the HTML page.
        String treeAsText = renderNodeAndItsDescendants(treeRoot, tree);

        // Mark the inventory object that was selected by the user
        String idSubstring = "[id: " + inventoryObjectId + "]";
        treeAsText = treeAsText.replace(idSubstring, idSubstring + " <----------------[YOUR SELECTION]");

        model.addAttribute("treeAsText", treeAsText);
        model.addAttribute("inventoryObjectId", inventoryObjectId);
        model.addAttribute(
                "inventoryObjectType",
                inventoryObject.getClass().getSimpleName().toLowerCase());

        // Rendered by the template by the same name + html suffix (the model is made available to the template)
        return "inventory-object-details";
    }

    private void insertDeviceAndItsDescendantDevicesIntoTree(MutableGraph<Object> tree, Device device) {

        tree.addNode(device);

        List<Device> childDevices = inventoryObjectRepository.findAllChildDevices(device);

        for (Device childDevice : childDevices) {

            insertDeviceAndItsDescendantDevicesIntoTree(tree, childDevice);
            tree.putEdge(device, childDevice);
        }
    }

    private void insertAlarmsIntoTree(MutableGraph<Object> tree) {

        // Alarms are only found on devices, so let's find all the devices in the tree
        Set<Device> allDevicesInTree = tree.nodes().stream()
                .filter(node -> node instanceof Device)
                .map(node -> (Device) node)
                .collect(Collectors.toSet());

        for (Device device : allDevicesInTree) {

            // Would be more efficient to find alarms for all devices at once - but would also be more complicated :-)
            List<Alarm> alarmsOnDevice = alarmRepository.findByDevice(device);

            for (Alarm alarm : alarmsOnDevice) {
                tree.putEdge(device, alarm);
            }
        }
    }

    private InventoryObject insertLocationAncestorsIntoTreeAndReturnRoot(
            MutableGraph<Object> tree, InventoryObject inventoryObject) {

        InventoryObject currentRoot = inventoryObject;

        String nextLocationIdToLookup;
        switch (inventoryObject) {
            case Device device -> nextLocationIdToLookup = device.getLocationId();
            case Location location -> nextLocationIdToLookup = location.getParentId();
            case Organization ignored -> {
                return inventoryObject;
            }
            case null, default ->
                throw new RuntimeException("Unexpected object type: "
                        + Objects.requireNonNull(inventoryObject).getClass().getSimpleName());
        }

        while (nextLocationIdToLookup != null) {
            Location location = inventoryObjectRepository.findLocationById(nextLocationIdToLookup);
            if (location != null) {
                tree.putEdge(location, currentRoot);
                currentRoot = location;
                nextLocationIdToLookup = location.getParentId();
            } else {
                nextLocationIdToLookup = null;
            }
        }
        return currentRoot;
    }

    private InventoryObject insertOrganizationIntoTreeAndReturnRoot(
            MutableGraph<Object> tree, InventoryObject currentRoot) {
        Organization organization = inventoryObjectRepository.findOrganization();
        if (currentRoot != null) {
            tree.putEdge(organization, currentRoot);
        } else {
            tree.addNode(organization);
        }
        return organization;
    }

    private static String renderNodeAndItsDescendants(Object treeRoot, MutableGraph<Object> tree) {
        StringBuilder treeAsText = new StringBuilder();
        renderNodeAndItsDescendants(treeRoot, tree, treeAsText, 0);
        return treeAsText.toString();
    }

    private static void renderNodeAndItsDescendants(
            Object node, MutableGraph<Object> tree, StringBuilder treeAsText, int depth) {

        treeAsText.append("   ".repeat(Math.max(0, depth)));

        treeAsText.append(createDescription(node)).append("\n");

        Set<Object> children = tree.successors(node);

        // Group the children into Alarms and InventoryObjects

        List<Alarm> childrenOfTypeAlarm = new ArrayList<>();
        List<InventoryObject> childrenOfTypeInventoryObjects = new ArrayList<>();

        for (Object child : children) {
            if (child instanceof Alarm alarm) {
                childrenOfTypeAlarm.add(alarm);
            } else if (child instanceof InventoryObject object) {
                childrenOfTypeInventoryObjects.add(object);
            } else {
                throw new RuntimeException(
                        "Tree contained unexpected type " + child.getClass().getSimpleName());
            }
        }

        // Render alarms first (we want them at the top - not mixed with sub devices)
        for (Alarm alarm : childrenOfTypeAlarm) {
            renderNodeAndItsDescendants(alarm, tree, treeAsText, depth + 1);
        }

        // Sort inventory objects alphabetically and then render them

        AlphanumComparator alphanumComparator = new AlphanumComparator();
        Comparator<InventoryObject> inventoryObjectComparator = (o1, o2) ->
                alphanumComparator.compare(o1 == null ? null : o1.getLabel(), o2 == null ? null : o2.getLabel());

        childrenOfTypeInventoryObjects.sort(inventoryObjectComparator);

        for (InventoryObject inventoryObject : childrenOfTypeInventoryObjects) {
            renderNodeAndItsDescendants(inventoryObject, tree, treeAsText, depth + 1);
        }
    }

    private static String createDescription(Object object) {

        return switch (object) {
            case Alarm alarm -> createAlarmDescription(alarm);
            case Device device -> createDeviceDescription(device);
            case Location location -> createLocationDescription(location);
            case Organization organization -> createOrganizationDescription(organization);
            case null, default ->
                throw new RuntimeException("Unexpected object type: "
                        + Objects.requireNonNull(object).getClass().getSimpleName());
        };
    }

    private static String createAlarmDescription(Alarm alarm) {
        return "* Alarm "
                + "(" + toLowerCaseString(alarm.getSeverity()) + ")"
                + " " + alarm.getLabel()
                + " -- " + alarm.getMessage()
                + " (active from " + alarm.getActivatedTime()
                + (alarm.getClearedTime() == null ? "" : " to " + alarm.getClearedTime())
                + ")"
                + " [id: " + alarm.getId() + "]";
    }

    private static String createDeviceDescription(Device device) {
        return "Device "
                + "(" + toLowerCaseString(device.getType()) + ")"
                + " " + device.getLabel()
                + " [id: " + device.getId() + "]";
    }

    private static String createLocationDescription(Location location) {
        return "Location "
                + "(" + toLowerCaseString(location.getType()) + ")"
                + " " + location.getLabel()
                + " [id: " + location.getId() + "]";
    }

    private static String createOrganizationDescription(Organization organization) {
        return "Organization " + organization.getLabel() + " [id: " + organization.getId() + "]";
    }

    private static String toLowerCaseString(String value) {
        if (value == null) {
            return "?";
        }
        return value.toLowerCase();
    }

    private static String toLowerCaseString(Enum value) {
        return toLowerCaseString(value != null ? value.toString() : null);
    }
}
