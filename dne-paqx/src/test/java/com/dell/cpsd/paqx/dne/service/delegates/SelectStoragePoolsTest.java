/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOData;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOProtectionDomain;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOStoragePool;
import com.dell.cpsd.paqx.dne.domain.vcenter.HostStorageDevice;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.service.common.client.exception.ServiceTimeoutException;
import com.dell.cpsd.service.engineering.standards.Device;
import com.dell.cpsd.service.engineering.standards.DeviceAssignment;
import com.dell.cpsd.service.engineering.standards.Error;
import com.dell.cpsd.service.engineering.standards.EssValidateStoragePoolResponseMessage;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAILS;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.SELECT_STORAGE_POOLS_FAILED;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */ public class SelectStoragePoolsTest
{
    private SelectStoragePools selectStoragePools;

    @Mock
    private NodeService nodeService;

    @Mock
    private DelegateExecution delegateExecution;

    private List<ScaleIOData> scaleIODataList = new ArrayList<>();
    List<NodeDetail>              nodeDetails       = new ArrayList<>();
    List<Device>                  newDevices        = new ArrayList<>();
    List<ScaleIOProtectionDomain> protectionDomains = new ArrayList<>();

    @Before
    public void setUp() throws Exception
    {
        selectStoragePools = Mockito.spy(new SelectStoragePools(nodeService));
        setupMockScaleIODataList();
        //        setupDiscoveredNodeInfo();
        setupNodeDetails();
        this.newDevices = setupNewDevices(1);
        setupProtectionDomains();
    }

    private void setupMockScaleIODataList()
    {
        List<ScaleIOData> scaleIODataList = new ArrayList<>();
        ScaleIOData scaleIOData = new ScaleIOData("sio1", "name1", "installId", "mdmMode", "systemVersion", "clusterState", "version");

        ScaleIOProtectionDomain protectionDomain1 = new ScaleIOProtectionDomain("PD-1", "pdName1", "ACTIVE");
        scaleIOData.addProtectionDomain(protectionDomain1);
        protectionDomain1.setScaleIOData(scaleIOData);
        scaleIODataList.add(scaleIOData);
        this.scaleIODataList = scaleIODataList;
    }

    /*private void setupDiscoveredNodeInfo()
    {
        List<DiscoveredNodeInfo> discoveredNodeInfoList = new ArrayList<>();
        DiscoveredNodeInfo discoveredNodeInfo1 = new DiscoveredNodeInfo("M-1", "MF-1", "P-1", "PF-1", "SN-1", "UUID-1");
        discoveredNodeInfoList.add(discoveredNodeInfo1);
        DiscoveredNodeInfo discoveredNodeInfo2 = new DiscoveredNodeInfo("M-2", "MF-2", "P-2", "PF-2", "SN-2", "UUID-2");
        discoveredNodeInfoList.add(discoveredNodeInfo2);
        this.discoveredNodeInfoList = discoveredNodeInfoList;
    }*/

    private void setupNodeDetails()
    {
        List<NodeDetail> nodeDetails = new ArrayList<>();
        NodeDetail nodeDetail1 = new NodeDetail("ND-1", "SN-1");
        nodeDetail1.setProtectionDomainId("PD-1");
        nodeDetails.add(nodeDetail1);
        NodeDetail nodeDetail2 = new NodeDetail("ND-2", "SN-2");
        nodeDetail2.setProtectionDomainId("PD-2");
        nodeDetails.add(nodeDetail2);
        this.nodeDetails = nodeDetails;
    }

    private List<Device> setupNewDevices(int i)
    {
        List<Device> mockDevices = new ArrayList<>();
        Device device1 = new Device("dev1Id-" + i, "dev1Name-" + i, "active", "dev1Serial-" + i, "dev1CanName", "dev1LogicalName",
                Device.Type.SSD, "1800000000");
        mockDevices.add(device1);
        Device device2 = new Device("dev1Id-" + (i + 1), "dev1Name-" + (i + 1), "active", "dev1Serial-" + (i + 1), "dev1CanName",
                "dev1LogicalName", Device.Type.SSD, "1800000000");
        mockDevices.add(device2);
        Device device3 = new Device("dev1Id-" + (i + 2), "dev1Name-" + (i + 2), "active", "dev1Serial-" + (i + 2), "dev1CanName",
                "dev1LogicalName", Device.Type.SSD, "1800000000");
        mockDevices.add(device3);
        return mockDevices;
    }

    private void setupProtectionDomains()
    {
        List<ScaleIOProtectionDomain> protectionDomains = new ArrayList<>();
        ScaleIOProtectionDomain protectionDomain1 = new ScaleIOProtectionDomain("PD-1", "pdName1", "ACTIVE");
        protectionDomain1.addStoragePool(new ScaleIOStoragePool());
        protectionDomains.add(protectionDomain1);
        ScaleIOProtectionDomain protectionDomain2 = new ScaleIOProtectionDomain("PD-2", "pdName2", "ACTIVE");
        protectionDomain2.addStoragePool(new ScaleIOStoragePool());
        protectionDomains.add(protectionDomain2);
        this.protectionDomains = protectionDomains;
    }

    @Test
    public void populateDeviceMaps_success()
    {
        Map<String, List<Device>> nodeToDeviceMap = new HashMap<>();
        Map<String, List<Device>> protectionDomainToDevicesMap = new HashMap<>();
        doReturn(newDevices).when(selectStoragePools).getNewDevices("ND-1");
        doReturn(newDevices).when(selectStoragePools).getNewDevices("ND-2");
        selectStoragePools.populateDeviceMaps(this.nodeDetails, nodeToDeviceMap, protectionDomainToDevicesMap);
        assertNotNull(nodeToDeviceMap);
        assertNotNull(protectionDomainToDevicesMap);
        assertEquals(2, nodeToDeviceMap.size());
        assertEquals(2, protectionDomainToDevicesMap.size());
        assertEquals(3, nodeToDeviceMap.get("ND-1").size());
        assertEquals(3, nodeToDeviceMap.get("ND-2").size());
        assertEquals(3, protectionDomainToDevicesMap.get("PD-1").size());
        assertEquals(3, protectionDomainToDevicesMap.get("PD-2").size());
    }

    @Test
    public void populateDeviceMaps_noProtectionDomainId()
    {
        Map<String, List<Device>> nodeToDeviceMap = new HashMap<>();
        Map<String, List<Device>> protectionDomainToDevicesMap = new HashMap<>();
        doReturn(newDevices).when(selectStoragePools).getNewDevices("ND-1");
        //        doReturn(newDevices).when(selectStoragePools).getNewDevices("ND-2");
        this.nodeDetails.get(1).setProtectionDomainId(null);
        try
        {
            selectStoragePools.populateDeviceMaps(this.nodeDetails, nodeToDeviceMap, protectionDomainToDevicesMap);
            fail("Expected IllegalStateException.");
        }
        catch (IllegalStateException e)
        {
            assertEquals("Could not find a valid protection domain for node: ND-2", e.getMessage());
        }
    }

    @Test
    public void validateStoragePoolsAndSetResponse_success() throws Exception
    {
        EssValidateStoragePoolResponseMessage storageResponseMessage = new EssValidateStoragePoolResponseMessage();
        storageResponseMessage.setWarnings(Collections.emptyList());
        storageResponseMessage.setErrors(Collections.emptyList());

        Map<String, DeviceAssignment> deviceToStoragePoolMap = new HashMap<>();
        deviceToStoragePoolMap.put("id-1", new DeviceAssignment("id-1", "sn-1", "ln-1", "dn-1", "spid-1", "spn-1"));
        deviceToStoragePoolMap.put("id-2", new DeviceAssignment("id-2", "sn-2", "ln-2", "dn-2", "spid-1", "spn-1"));
        storageResponseMessage.setDeviceToStoragePoolMap(deviceToStoragePoolMap);
        doReturn(storageResponseMessage).when(nodeService).validateStoragePools(anyList(), anyList(), anyMap(), any());

        Map<String, DeviceAssignment> deviceMap = new HashMap<>();
        selectStoragePools.validateStoragePoolsAndSetResponse(deviceMap, newDevices, new HashMap<String, Map<String, HostStorageDevice>>(),
                protectionDomains, "PD-1", null);
        assertFalse(deviceMap.isEmpty());
        assertEquals(2, deviceMap.size());
    }

    @Test
    public void validateStoragePoolsAndSetResponse_noProtectionDomain() throws Exception
    {
        try
        {
            selectStoragePools
                    .validateStoragePoolsAndSetResponse(new HashMap<>(), newDevices, new HashMap<String, Map<String, HostStorageDevice>>(),
                            protectionDomains, "PD-3", null);
            fail("Expected IllegalStateException with no protection domain found.");
        }
        catch (IllegalStateException e)
        {
            assertEquals("Could not find a valid protection domain", e.getMessage());
        }
    }

    @Ignore
    @Test
    public void validateStoragePoolsAndSetResponse_addDummyPool() throws Exception
    {
        EssValidateStoragePoolResponseMessage storageResponseMessage = new EssValidateStoragePoolResponseMessage();
        storageResponseMessage.setWarnings(Collections.emptyList());
        storageResponseMessage.setErrors(Collections.emptyList());

        Map<String, DeviceAssignment> deviceToStoragePoolMap = new HashMap<>();
        deviceToStoragePoolMap.put("id-1", new DeviceAssignment("id-1", "sn-1", "ln-1", "dn-1", "spid-1", "spn-1"));
        deviceToStoragePoolMap.put("id-2", new DeviceAssignment("id-2", "sn-2", "ln-2", "dn-2", "spid-1", "spn-1"));
        storageResponseMessage.setDeviceToStoragePoolMap(deviceToStoragePoolMap);
        List<Error> errors = new ArrayList<>();
        Error error = new Error("", "No Storage pool found.");
        errors.add(error);
        storageResponseMessage.setErrors(errors);
        doReturn(storageResponseMessage).when(nodeService).validateStoragePools(anyList(), anyList(), anyMap(), null);

        Map<String, DeviceAssignment> deviceMap = new HashMap<>();
        selectStoragePools.validateStoragePoolsAndSetResponse(deviceMap, newDevices, new HashMap<String, Map<String, HostStorageDevice>>(),
                protectionDomains, "PD-1", null);
        assertTrue(deviceMap.isEmpty());
        assertEquals(1, protectionDomains.get(0).getStoragePools().size());
    }

    @Test
    public void delegateExecute_nullNodeDetails()
    {

        doReturn(null).when(delegateExecution).getVariable(NODE_DETAILS);
        try
        {
            selectStoragePools.delegateExecute(delegateExecution);
            fail("Expecting node details not found error.");
        }
        catch (BpmnError ex)
        {
            assertEquals(SELECT_STORAGE_POOLS_FAILED, ex.getErrorCode());
            assertEquals("The List of Node Detail was not found!  Please add at least one Node Detail and try again.", ex.getMessage());
        }
        catch (Exception e)
        {
            fail("Unexpected exception.");
        }

    }

    @Test
    public void delegateExecute_noNewDeviceForProtectionDomain() throws Exception
    {

        doReturn(nodeDetails).when(delegateExecution).getVariable(NODE_DETAILS);
        try
        {
            selectStoragePools.delegateExecute(delegateExecution);
            fail("Expecting no devices found for protection domain.");
        }
        catch (BpmnError ex)
        {
            assertEquals(SELECT_STORAGE_POOLS_FAILED, ex.getErrorCode());
            assertEquals("No disks found in the node inventory data.", ex.getMessage());
        }
        catch (Exception e)
        {
            fail("Unexpected exception.");
        }

    }

    @Test
    public void delegateExecute_timeoutException() throws Exception
    {

        doReturn(nodeDetails).when(delegateExecution).getVariable(NODE_DETAILS);
        doReturn(newDevices).when(selectStoragePools).getNewDevices(anyString());
        doReturn(new HashMap<String, Map<String, HostStorageDevice>>()).when(nodeService).getHostToStorageDeviceMap(anyList());
        doReturn(scaleIODataList).when(nodeService).listScaleIOData();
        doThrow(new ServiceTimeoutException()).when(selectStoragePools)
                .validateStoragePoolsAndSetResponse(anyMap(), anyList(), anyMap(), anyList(), anyString(), any());
        try
        {
            selectStoragePools.delegateExecute(delegateExecution);
            fail("Expecting service timeout exception.");
        }
        catch (BpmnError ex)
        {
            assertEquals(SELECT_STORAGE_POOLS_FAILED, ex.getErrorCode());
            assertEquals("Error validating storage pool(s) for protection domain: PD-2", ex.getMessage());
        }
        catch (Exception e)
        {
            fail("Unexpected exception.");
        }
    }

    @Test
    public void delegateExecute_success() throws Exception
    {

        doReturn(nodeDetails).when(delegateExecution).getVariable(NODE_DETAILS);
        doReturn(newDevices).when(selectStoragePools).getNewDevices("ND-1");
        doReturn(setupNewDevices(4)).when(selectStoragePools).getNewDevices("ND-2");
        doReturn(new HashMap<String, Map<String, HostStorageDevice>>()).when(nodeService).getHostToStorageDeviceMap(anyList());
        doReturn(scaleIODataList).when(nodeService).listScaleIOData();
        doAnswer(new Answer<Object>()
        {
            public Object answer(InvocationOnMock invocation)
            {
                if (((String) invocation.getArguments()[4]).equals("PD-1"))
                {
                    Map<String, DeviceAssignment> deviceMap = (Map<String, DeviceAssignment>) invocation.getArguments()[0];
                    deviceMap.put("dev1Id-1",
                            new DeviceAssignment("dev1Id-1", "dev1Serial-1", "dev1LogicalName", "dev1Name-1", "SP-1", "StoragePool-1"));
                    deviceMap.put("dev1Id-2",
                            new DeviceAssignment("dev1Id-2", "dev1Serial-2", "dev1LogicalName", "dev1Name-2", "SP-1", "StoragePool-1"));
                    deviceMap.put("dev1Id-3",
                            new DeviceAssignment("dev1Id-3", "dev1Serial-3", "dev1LogicalName", "dev1Name-3", "SP-1", "StoragePool-1"));
                }
                else
                {
                    Map<String, DeviceAssignment> deviceMap = (Map<String, DeviceAssignment>) invocation.getArguments()[0];
                    deviceMap.put("dev1Id-4",
                            new DeviceAssignment("dev1Id-4", "dev1Serial-4", "dev1LogicalName", "dev1Name-4", "SP-2", "StoragePool-2"));
                    deviceMap.put("dev1Id-5",
                            new DeviceAssignment("dev1Id-5", "dev1Serial-5", "dev1LogicalName", "dev1Name-5", "SP-2", "StoragePool-2"));
                    deviceMap.put("dev1Id-6",
                            new DeviceAssignment("dev1Id-6", "dev1Serial-6", "dev1LogicalName", "dev1Name-6", "SP-2", "StoragePool-2"));
                }

                return null;
            }
        }).when(selectStoragePools).validateStoragePoolsAndSetResponse(anyMap(), anyList(), anyMap(), anyList(), anyString(), any());

        selectStoragePools.delegateExecute(delegateExecution);
        assertNotNull(nodeDetails.get(0).getDeviceToDeviceStoragePool());
        assertNotNull(nodeDetails.get(1).getDeviceToDeviceStoragePool());
        assertEquals(3, nodeDetails.get(0).getDeviceToDeviceStoragePool().size());
        assertEquals(3, nodeDetails.get(1).getDeviceToDeviceStoragePool().size());
        nodeDetails.get(0).getDeviceToDeviceStoragePool().entrySet().stream().forEach(entry -> {
            assertThat(entry.getKey(), anyOf(equalTo("dev1Id-1"), equalTo("dev1Id-2"), equalTo("dev1Id-3")));
            assertThat(entry.getValue().getDeviceId(), anyOf(equalTo("dev1Id-1"), equalTo("dev1Id-2"), equalTo("dev1Id-3")));
        });
        nodeDetails.get(1).getDeviceToDeviceStoragePool().entrySet().stream().forEach(entry -> {
            assertThat(entry.getKey(), anyOf(equalTo("dev1Id-4"), equalTo("dev1Id-5"), equalTo("dev1Id-6")));
            assertThat(entry.getValue().getDeviceId(), anyOf(equalTo("dev1Id-4"), equalTo("dev1Id-5"), equalTo("dev1Id-6")));
        });
    }

}