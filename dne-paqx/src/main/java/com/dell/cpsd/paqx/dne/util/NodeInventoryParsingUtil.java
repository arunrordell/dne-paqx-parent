/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.util;

import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.service.engineering.standards.Device;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeInventoryParsingUtil
{
    private static final String DMI_FIELD          = "dmi";
    private static final String SOURCE_FIELD       = "source";
    private static final String DATA_FIELD         = "data";
    private static final String SYSTEM_INFO_FIELD  = "System Information";
    private static final String SERIAL_NUM_FIELD   = "Serial Number";
    private static final String PRODUCT_FIELD      = "Product Name";
    private static final String FAMILY_FIELD       = "Family";
    private static final String SMART_SOURCE       = "smart";
    private static final String SOLID_STATE_DEVICE = "Solid State Device";

    public static String getDevicePathById()
    {
        return DEVICE_PATH_BY_ID;
    }

    private static final String DEVICE_PATH_BY_ID = "/dev/disk/by-id/wwn-";

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeInventoryParsingUtil.class);

    public static DiscoveredNodeInfo getDiscoveredNodeInfo(Object nodeInventory, String uuid)
    {
        if (nodeInventory == null)
        {
            return null;
        }

        List inventoryList = (ArrayList) nodeInventory;
        for (Object obj : inventoryList)
        {
            Map map = (HashMap) obj;
            if (((String) map.get(SOURCE_FIELD)).equalsIgnoreCase(DMI_FIELD))
            {
                Map data = (HashMap) map.get(DATA_FIELD);
                Map sysInfo = (HashMap) data.get(SYSTEM_INFO_FIELD);
                String serialNumber = (String) sysInfo.get(SERIAL_NUM_FIELD);
                String product = (String) sysInfo.get(PRODUCT_FIELD);
                String family = (String) sysInfo.get(FAMILY_FIELD);
                return new DiscoveredNodeInfo(product, family, product, family, serialNumber, uuid);
            }
        }
        return null;
    }

    public static List<Device> parseNewDevices(String jsonString)
    {
        List<Device> newDevices = null;
        if (jsonString != null)
        {
            newDevices = new ArrayList<>();
            DocumentContext context = JsonPath.parse(jsonString.toLowerCase());
            int length = context.read("$.length()");
            String source;
            int dataLen;
            String deviceId;
            String deviceName;
            String ssdDeviceType;
            String serialNumber;
            String devicePath = null;
            String capacity;
            for (int iCount = 0; iCount < length; iCount++)
            {
                source = context.read("$[" + iCount + "]['source']", String.class);
                if (SMART_SOURCE.equalsIgnoreCase(source))
                {
                    dataLen = context.read("$[" + iCount + "]['data'].length()");
                    for (int iDataCount = 0; iDataCount < dataLen; iDataCount++)
                    {
                        try
                        {
                            // reset device id
                            deviceId = null;
                            ssdDeviceType = context
                                    .read("$[" + iCount + "]['data'][" + iDataCount + "]['smart']['identity']['rotation rate']",
                                            String.class);
                            if (SOLID_STATE_DEVICE.equalsIgnoreCase(ssdDeviceType))
                            {
                                deviceName = context.read("$[" + iCount + "]['data'][" + iDataCount + "]['os device name']", String.class);
                                serialNumber = context
                                        .read("$[" + iCount + "]['data'][" + iDataCount + "]['smart']['identity']['serial number']",
                                                String.class);
                                capacity = context
                                        .read("$[" + iCount + "]['data'][" + iDataCount + "]['smart']['identity']['user capacity']",
                                                String.class);
                                capacity = capacity == null ? null : capacity.split("bytes")[0].replaceAll(",", "").trim();

                                // skip SATADOM-ML drives, they are not scaleio data disks
                                try
                                {
                                    deviceId = context
                                            .read("$[" + iCount + "]['data'][" + iDataCount + "]['smart']['identity']['logical unit id']",
                                                    String.class);

                                    //It has to include 0x
                                    devicePath = DEVICE_PATH_BY_ID + deviceId;

                                    // remove 0x prefix
                                    deviceId = deviceId == null ? null : deviceId.substring(2);
                                }
                                catch (PathNotFoundException e)
                                {
                                    try
                                    {
                                        deviceId = context.read("$[" + iCount + "]['data'][" + iDataCount
                                                + "]['smart']['identity']['lu wwn device id']", String.class);
                                        // remove spaces in between
                                        deviceId = deviceId == null ? null : deviceId.replaceAll("\\s", "");

                                        devicePath = DEVICE_PATH_BY_ID + deviceId;
                                    }
                                    catch (PathNotFoundException ex)
                                    {
                                        LOGGER.info("No device id found for device name: " + deviceName);
                                    }
                                }

                                LOGGER.info("Device name " + deviceName + " has device path as " + devicePath);

                                if (deviceId != null)
                                {
                                    Device newDevice = new Device(deviceId, deviceName, null, serialNumber, null, devicePath,
                                            Device.Type.SSD, capacity);
                                    newDevices.add(newDevice);
                                }
                            }
                        }
                        catch (PathNotFoundException e)
                        {
                            //Do nothing and continue
                            LOGGER.error("Could not parse node inventory field : ", e);
                        }
                    }
                    break;
                }
            }

        }

        return newDevices;
    }
}
