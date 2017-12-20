/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.transformers;

import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIODevice;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOSDS;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOStoragePool;
import com.dell.cpsd.paqx.dne.domain.vcenter.Host;
import com.dell.cpsd.paqx.dne.domain.vcenter.HostStorageDevice;
import com.dell.cpsd.service.engineering.standards.Device;
import com.dell.cpsd.service.engineering.standards.EssValidateStoragePoolRequestMessage;
import com.dell.cpsd.service.engineering.standards.StoragePool;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for storage transformer.
 * <p>
 * <p>
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */

public class StoragePoolEssRequestTransformerTest
{

    private StoragePoolEssRequestTransformer transformer;
    private List<ScaleIOStoragePool>         listScaleIOStoragePool;
    private List<ScaleIODevice>              listDevices;

    @Before
    public void setUp() throws Exception
    {
        transformer = new StoragePoolEssRequestTransformer();
    }

    @Test
    public void getHostToStorageDeviceMap_success()
    {

        List<Host> hostList = new ArrayList<>();
        Host host1 = buildHost("fpr1-h13.lab.vce.com", 4, true);
        hostList.add(host1);
        Host host2 = buildHost("fpr1-h11.lab.vce.com", 5, false);
        hostList.add(host2);

        Map<String, Map<String, HostStorageDevice>> map = transformer.getHostToStorageDeviceMap(hostList);
        assertNotNull(map.get("fpr1-h11.lab.vce.com-ESX"));
        assertNotNull(map.get("fpr1-h13.lab.vce.com-ESX"));
        assertEquals(5, map.get("fpr1-h11.lab.vce.com-ESX").size());
        assertEquals(4, map.get("fpr1-h13.lab.vce.com-ESX").size());

        map.get("fpr1-h13.lab.vce.com-ESX").forEach((key, value) -> {
            assertThat(key, containsString("displayName"));
            assertEquals(key, value.getDisplayName());
        });

        map.get("fpr1-h11.lab.vce.com-ESX").forEach((key, value) -> {
            assertThat(key, containsString("displayName"));
            assertEquals(key, value.getDisplayName());
        });

    }

    @Test
    public void getHostToStorageDeviceMap_emptyHosts()
    {

        List<Host> hostList = new ArrayList<>();

        Map<String, Map<String, HostStorageDevice>> map = transformer.getHostToStorageDeviceMap(hostList);
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    public void correlateScaleIOAndVcenterData_success()
    {
        Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap = buildHostToStorageDeviceMap(true);

        ScaleIOSDS sds = buildScaleIOSDS("fpr1-h12.lab.vce.com-ESX");

        Map<String, HostStorageDevice> actual = transformer.correlateScaleIOAndVcenterData(sds, hostToStorageDeviceMap);

        assertNotNull(actual);
        assertEquals(3, actual.size());
        actual.forEach((key, value) -> {
            assertThat(key, anyOf(is("displayName6"), is("displayName7"), is("displayName8")));
        });
    }

    @Test
    public void correlateScaleIOAndVcenterData_notFound()
    {
        Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap = buildHostToStorageDeviceMap(true);
        ScaleIOSDS sds = buildScaleIOSDS("fpr1-h14.lab.vce.com-ESX");

        try
        {
            Map<String, HostStorageDevice> actual = transformer.correlateScaleIOAndVcenterData(sds, hostToStorageDeviceMap);
            assertEquals(null, actual);
        }
        catch (NoSuchElementException ex)
        {
            fail("Error when no correlation found between ScaleIO sds and vcenter.");
        }
    }

    @Test
    public void collectDevicesInPool_allFlash()
    {
        Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap = buildHostToStorageDeviceMap(true);
        ScaleIOStoragePool scaleIOStoragePool = buildScaleIOStoragePool("1", 12);

        StoragePool storagePool = transformer.collectDevicesInPool(scaleIOStoragePool, hostToStorageDeviceMap, null);

        assertNotNull(storagePool);
        assertEquals("1", storagePool.getId());
        assertEquals("SP-1", storagePool.getName());
        assertEquals("12", storagePool.getNumberOfDevices());
        assertEquals(12, storagePool.getDevices().size());
        assertEquals(StoragePool.Type.SSD, storagePool.getType());

        storagePool.getDevices().stream().forEach(device -> {
            assertEquals(Device.Type.SSD, device.getType());
        });
    }

    @Test
    public void collectDevicesInPool_noFlash()
    {
        Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap = buildHostToStorageDeviceMap(false);
        ScaleIOStoragePool scaleIOStoragePool = buildScaleIOStoragePool("1", 12);

        StoragePool storagePool = transformer.collectDevicesInPool(scaleIOStoragePool, hostToStorageDeviceMap, null);

        assertNotNull(storagePool);
        assertEquals("1", storagePool.getId());
        assertEquals("SP-1", storagePool.getName());
        assertEquals("12", storagePool.getNumberOfDevices());
        assertEquals(12, storagePool.getDevices().size());
        assertEquals(StoragePool.Type.HDD, storagePool.getType());

        storagePool.getDevices().stream().forEach(device -> {
            assertEquals(Device.Type.HDD, device.getType());
        });
    }

    @Test
    public void transform_success()
    {
        Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap = buildHostToStorageDeviceMap(true);
        ScaleIOStoragePool scaleIOStoragePool = buildScaleIOStoragePool("1", 12);

        EssValidateStoragePoolRequestMessage request = transformer
                .transform(Collections.singletonList(scaleIOStoragePool), hostToStorageDeviceMap, null);
        assertNotNull(request.getStoragePools());
        assertEquals(1, request.getStoragePools().size());
        assertNotNull(request.getStoragePools().get(0).getDevices());
        assertEquals(12, request.getStoragePools().get(0).getDevices().size());
    }

    private ScaleIOStoragePool buildScaleIOStoragePool(String id, int numberOfDevices)
    {
        ScaleIOStoragePool storagePool = new ScaleIOStoragePool();
        storagePool.setId(id);
        storagePool.setName("SP-" + id);
        storagePool.setUseRfcache(false);
        storagePool.setUseRmcache(false);
        storagePool.setZeroPaddingEnabled(true);
        List<ScaleIODevice> devices = new ArrayList<>();
        ScaleIODevice device = null;
        int j = 0;
        for (int i = 0; i < numberOfDevices; i++)
        {
            device = new ScaleIODevice("" + i, "displayName" + i, null, "Normal");
            device.setSds(new ScaleIOSDS());
            device.getSds().setName("fpr1-h1" + j + ".lab.vce.com-ESX");
            if ((i + 1) % 3 == 0)
            {
                j++;
            }
            devices.add(device);
        }
        storagePool.setDevices(devices);
        return storagePool;
    }

    private Host buildHost(String name, int numberOfDevices, boolean allFlash)
    {
        Host host = new Host();
        host.setName(name + "-ESX");

        List<HostStorageDevice> hostStorageDeviceList = new ArrayList<>();
        HostStorageDevice hostStorageDevice = null;
        for (int i = 0; i < numberOfDevices; i++)
        {
            hostStorageDeviceList.add(buildHostStorageDevice("" + i, allFlash));
        }
        host.setHostStorageDeviceList(hostStorageDeviceList);
        return host;
    }

    private HostStorageDevice buildHostStorageDevice(String id, boolean ssd)
    {
        return new HostStorageDevice("displayName" + id, ssd, "sn" + id, "naa." + id);
    }

    private ScaleIOSDS buildScaleIOSDS(String id)
    {
        return new ScaleIOSDS("id-" + id, id, null, 8800);
    }

    private Map<String, Map<String, HostStorageDevice>> buildHostToStorageDeviceMap(boolean allFlash)
    {
        Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap = new HashMap<>();

        Map<String, HostStorageDevice> hostStorageDeviceMap1 = new HashMap<>();
        hostStorageDeviceMap1.put("displayName0", buildHostStorageDevice("0", allFlash));
        hostStorageDeviceMap1.put("displayName1", buildHostStorageDevice("1", allFlash));
        hostStorageDeviceMap1.put("displayName2", buildHostStorageDevice("2", allFlash));
        hostToStorageDeviceMap.put("fpr1-h10.lab.vce.com", hostStorageDeviceMap1);

        Map<String, HostStorageDevice> hostStorageDeviceMap2 = new HashMap<>();
        hostStorageDeviceMap2.put("displayName3", buildHostStorageDevice("3", allFlash));
        hostStorageDeviceMap2.put("displayName4", buildHostStorageDevice("4", allFlash));
        hostStorageDeviceMap2.put("displayName5", buildHostStorageDevice("5", allFlash));
        hostToStorageDeviceMap.put("fpr1-h11.lab.vce.com", hostStorageDeviceMap2);

        Map<String, HostStorageDevice> hostStorageDeviceMap3 = new HashMap<>();
        hostStorageDeviceMap3.put("displayName6", buildHostStorageDevice("6", allFlash));
        hostStorageDeviceMap3.put("displayName7", buildHostStorageDevice("7", allFlash));
        hostStorageDeviceMap3.put("displayName8", buildHostStorageDevice("8", allFlash));
        hostToStorageDeviceMap.put("fpr1-h12.lab.vce.com", hostStorageDeviceMap3);

        Map<String, HostStorageDevice> hostStorageDeviceMap4 = new HashMap<>();
        hostStorageDeviceMap4.put("displayName9", buildHostStorageDevice("9", allFlash));
        hostStorageDeviceMap4.put("displayName10", buildHostStorageDevice("10", allFlash));
        hostStorageDeviceMap4.put("displayName11", buildHostStorageDevice("11", allFlash));
        hostToStorageDeviceMap.put("fpr1-h13.lab.vce.com", hostStorageDeviceMap4);

        return hostToStorageDeviceMap;
    }

}
