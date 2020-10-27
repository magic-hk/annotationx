/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ict.annotationx.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.ict.annotationx.intf.AnnotationXService;
import com.ict.annotationx.intf.Importance;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.InvalidFieldException;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.DeviceAnnotationConfig;
import org.onosproject.net.device.DeviceStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.net.AnnotationKeys.OWNER;
import static org.onosproject.net.AnnotationKeys.RACK_ADDRESS;
import static org.onosproject.net.config.basics.BasicElementConfig.GRID_X;
import static org.onosproject.net.config.basics.BasicElementConfig.GRID_Y;
import static org.onosproject.net.config.basics.BasicElementConfig.LATITUDE;
import static org.onosproject.net.config.basics.BasicElementConfig.LOC_TYPE;
import static org.onosproject.net.config.basics.BasicElementConfig.LONGITUDE;
import static org.onosproject.net.config.basics.BasicElementConfig.UI_TYPE;
import static org.onosproject.net.config.basics.BasicElementConfig.NAME;

/**
 * description TODO.
 *
 * @author HuangKai, njnu_cs_hk@qq.com
 * @version 1.0
 * @since 19-3-19 下午8:25
 */
public class AbstractAnnotationXService implements AnnotationXService {

    protected NetworkConfigService networkConfigService;
    protected DeviceStore deviceStore;
    private static final Map<String, String> IMPORTANCE_MAP = new HashMap<>();

    private static final String DEVICES = "devices";
    private static final String PORTS = "ports";
    private static final String ID = "id";
    private static final String ANNOTATIONS = "annotations";

    //refer to BasicDeviceConfig.java
    private static final String TYPE = "type";
    private static final String DRIVER = "driver";
    private static final String MANAGEMENT_ADDRESS = "managementAddress";
    private static final String MANUFACTURER = "manufacturer";
    private static final String HW_VERSION = "hwVersion";
    private static final String SW_VERSION = "swVersion";
    private static final String SERIAL = "serial";
    private static final String DEVICE_KEY_ID = "deviceKeyId";
    // addontional add refer to BasicElementConfig and AllowedEntityConfig
    private static final String ALLOWED = "allowed";
    private static final String ROLES = "roles";
    // length check
    private static final int DRIVER_MAX_LENGTH = 256;
    private static final int MANUFACTURER_MAX_LENGTH = 256;
    /*private static final int HW_VERSION_MAX_LENGTH = 256;
    private static final int SW_VERSION_MAX_LENGTH = 256;
    private static final int SERIAL_MAX_LENGTH = 256;*/
    private static final int MANAGEMENT_ADDRESS_MAX_LENGTH = 1024;


    // refer to BasicElementConfig.java
    private static final int NAME_MAX_LENGTH = 256;
    private static final int UI_TYPE_MAX_LENGTH = 128;
    private static final int LOC_TYPE_MAX_LENGTH = 32;
    private static final int RACK_ADDRESS_MAX_LENGTH = 256;
    private static final int OWNER_MAX_LENGTH = 128;

    private static final String ENTRIES = "entries";
    private static final String EMPTY_STRING = "";

    // myself annotation extend
    private static final String IMPORTANCE = "importance";

    protected void init() {
        for (Importance e : Importance.values()) {
            IMPORTANCE_MAP.put(e.getName(), e.getDescription());
        }

    }

    @Override
    public List<String> updateDeviceAnnotations(ObjectNode root) {
        List<String> errorMsgs = new ArrayList<String>();
        JsonNode devicesNode = root.get(DEVICES);
        if (devicesNode == null) {
            errorMsgs.add(subjectNotFoundErrorString(DEVICES));
        } else {
            int index = 0;
            for (JsonNode jsonNode : devicesNode) {
                // protect
                if (errorMsgs.size() > 10) {
                    break;
                }
                index++;
                ObjectMapper mapper = new ObjectMapper();
                Optional<String> id = stringFiledValue(jsonNode, ID);
                if (!id.isPresent()) {
                    errorMsgs.add(attributeNotFoundErrorString(index, ID));
                    continue;
                }
                DeviceId did = DeviceId.deviceId(id.get());
                Device device = deviceStore.getDevice(did);
                //check device if in store
                if (device == null) {
                    errorMsgs.add(attributeNotFoundErrorString(index, ID + ":" + id.get()));
                    continue;
                }
                Map<String, String> extentions;
                try {
                    //get extension annotions
                    extentions = mapper.convertValue(jsonNode.path(ANNOTATIONS), Map.class);
                    if (extentions == null || extentions.size() == 0) {
                        errorMsgs.add(attributeNotFoundErrorString(index, ANNOTATIONS));
                        continue;
                    }
                    DeviceAnnotationConfig cfg = networkConfigService.getConfig(did, DeviceAnnotationConfig.class);
                    if (cfg == null) {
                        cfg = new DeviceAnnotationConfig(did);
                    }
                    // iterator every annotion
                    for (Map.Entry<String, String> entry: extentions.entrySet()) {
                        //TODO check key and value
                        cfg.annotation(entry.getKey(), entry.getValue());
                    }
                    // updates all or nothing
                    List<String> validateErrorMsgs = validateDeviceAnnotations(index, cfg);
                    if (validateErrorMsgs.size() > 0) {
                        errorMsgs.addAll(validateErrorMsgs);
                        continue;
                    }
                    networkConfigService.applyConfig(did, DeviceAnnotationConfig.class, cfg.node());
                } catch (Exception e) {
                    errorMsgs.add(indexPrefix(index) + "Key Or Value Type Error, all should be String");
                    continue;
                }
            }
        }
        return errorMsgs;
    }

    // refer to BasicDeviceConfig BasicElementConfig Config
    public List<String> validateDeviceAnnotations(int index, DeviceAnnotationConfig cfg) {
        List<String> errorMsgs = new ArrayList<String>();
        ObjectNode object = cfg.node() instanceof ObjectNode ? (ObjectNode) cfg.node()  : null;
        if (object == null) {
            errorMsgs.add(subjectNotFoundErrorString(index));
            return errorMsgs;
        }
        JsonNode node = object.path(ENTRIES);
        try {
            // here is refer to BasicDeviceConfig isVaild()
            hasOnlyFields(node, ALLOWED, NAME, LOC_TYPE, LATITUDE, LONGITUDE,
                    GRID_Y, GRID_X, UI_TYPE, RACK_ADDRESS, OWNER, TYPE, DRIVER, ROLES,
                    MANUFACTURER, HW_VERSION, SW_VERSION, SERIAL,
                    MANAGEMENT_ADDRESS, DEVICE_KEY_ID, IMPORTANCE);
            isValidImportance(node);
            isValidVauleRange(node, LATITUDE, -90.0, 90.0);
            isValidVauleRange(node, LONGITUDE, -180, 180);
            isValidLength(node, DRIVER, DRIVER_MAX_LENGTH);
            isValidLength(node, MANUFACTURER, MANUFACTURER_MAX_LENGTH);
            isValidLength(node, HW_VERSION, MANUFACTURER_MAX_LENGTH);
            isValidLength(node, SW_VERSION, MANUFACTURER_MAX_LENGTH);
            isValidLength(node, SERIAL, MANUFACTURER_MAX_LENGTH);
            isValidLength(node, MANAGEMENT_ADDRESS, MANAGEMENT_ADDRESS_MAX_LENGTH);
            isValidLength(node, NAME, NAME_MAX_LENGTH);
            isValidLength(node, UI_TYPE, UI_TYPE_MAX_LENGTH);
            isValidLength(node, LOC_TYPE, LOC_TYPE_MAX_LENGTH);
            isValidLength(node, RACK_ADDRESS, RACK_ADDRESS_MAX_LENGTH);
            isValidLength(node, OWNER, OWNER_MAX_LENGTH);
        } catch (Exception e) {
            errorMsgs.add(indexPrefix(index) + e.getMessage());
        }
        return errorMsgs;
    }

    protected boolean isValidVauleRange(JsonNode node, String field, double min, double max)
            throws InvalidFieldException {
        String value = node.path(field).asText();
        if (value == null) {
            return true;
        }
        try {
            Double doubleValue = Double.valueOf(value);
            if (doubleValue < min || doubleValue > max) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new InvalidFieldException(field, "Field range should be in [" + min + "," + max + "]");
        }
        return true;
    }

    protected  String indexPrefix(int index) {
        return "Config for Object " + index + ":";
    }

    protected boolean hasOnlyFields(JsonNode node, String... allowedFields) throws InvalidFieldException {
        Set<String> fields = ImmutableSet.copyOf(allowedFields);
        node.fieldNames().forEachRemaining(f -> {
            if (!fields.contains(f)) {
                throw new InvalidFieldException(f, "Field is not allowed");
            }
        });
        return true;
    }

    protected boolean isValidLength(JsonNode node, String field, int maxLength) throws InvalidFieldException {
        if (node.path(field).asText(EMPTY_STRING).length() > maxLength) {
            throw new InvalidFieldException(field, "exceeds maximum length " + maxLength);
        }
        return true;
    }

    protected boolean isValidImportance(JsonNode node) throws InvalidFieldException {
        String value = node.path(IMPORTANCE).asText(EMPTY_STRING);
        if (IMPORTANCE_MAP.get(value) == null) {
            throw new InvalidFieldException(IMPORTANCE, "value " + value + " not exist");
        }
        return true;
    }



    // error message
    private String subjectNotFoundErrorString(String subject) {
        return "Config for '" + subject + "' not found";
    }

    private String attributeNotFoundErrorString(int index, String subject) {
        return "Config for Object " + index + "'s " + subject + " not found";
    }

    private String subjectNotFoundErrorString(int index) {
        return "Config for Object " + index + " not found";
    }

    private Optional<String> stringFiledValue(JsonNode jsonNode, String field) {
        Optional<String> value;
        if (jsonNode.get(field) == null) {
            value = Optional.empty();
        } else {
            value = Optional.of(jsonNode.get(field).asText());
        }
        return value;
    }
}
