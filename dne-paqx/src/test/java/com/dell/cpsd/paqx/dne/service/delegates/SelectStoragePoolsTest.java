/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOData;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOProtectionDomain;
import com.dell.cpsd.paqx.dne.exception.TaskResponseFailureException;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import com.dell.cpsd.service.common.client.exception.ServiceTimeoutException;
import com.dell.cpsd.service.engineering.standards.Device;
import com.dell.cpsd.service.engineering.standards.DeviceAssignment;
import com.dell.cpsd.service.engineering.standards.Error;
import com.dell.cpsd.service.engineering.standards.EssValidateStoragePoolResponseMessage;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
public class SelectStoragePoolsTest
{
    private SelectStoragePools selectStoragePools;

    private NodeDetail nodeDetail;

    @Mock
    private NodeService nodeService;

    @Mock
    private DelegateExecution delegateExecution;

    @Before
    public void setUp() throws Exception
    {
        selectStoragePools = Mockito.spy(new SelectStoragePools(nodeService));
        nodeDetail = new NodeDetail("1", "abc");
        nodeDetail.setProtectionDomainId("pdId1");
        doReturn(nodeDetail).when(delegateExecution).getVariable(NODE_DETAIL);
        setupMockScaleIODataList();
    }

    @Test
    public void testSuccessful() throws ServiceTimeoutException, ServiceExecutionException
    {
        mockPopulatedNewDevices();
        mockFoundValidStoragePoolResponse();

        selectStoragePools.delegateExecute(delegateExecution);
        Map<String, DeviceAssignment> deviceAssignment = nodeDetail.getDeviceToDeviceStoragePool();
        assertFalse(deviceAssignment.isEmpty());
    }

    @Test(expected = BpmnError.class)
    public void testEmptyDisks() throws ServiceTimeoutException, ServiceExecutionException
    {
        mockUnpopulatedNewDevices();
        selectStoragePools.delegateExecute(delegateExecution);
    }

    @Test
    public void testNoValidStoragePoolsInitially() throws ServiceTimeoutException, ServiceExecutionException, TaskResponseFailureException
    {
        mockPopulatedNewDevices();
        setupInvalidStoragePoolMock();

        selectStoragePools.delegateExecute(delegateExecution);
        Map<String, DeviceAssignment> deviceAssignment = nodeDetail.getDeviceToDeviceStoragePool();
        assertFalse(deviceAssignment.isEmpty());
        deviceAssignment.values().stream().filter(Objects::nonNull)
                .forEach(da -> assertTrue(da.getStoragePoolId() == null && da.getStoragePoolName().equals("temp")));
    }

    private void setupInvalidStoragePoolMock() throws ServiceTimeoutException, ServiceExecutionException
    {
        EssValidateStoragePoolResponseMessage invalidStoragePoolResponse = setupInvalidStoragePoolResponse();

        doReturn(invalidStoragePoolResponse).when(nodeService)
                .validateStoragePools(Mockito.anyList(), Mockito.anyList(), Mockito.anyMap());
    }

    private EssValidateStoragePoolResponseMessage setupInvalidStoragePoolResponse()
    {
        Map<String, DeviceAssignment> emptyDeviceToStoragePoolMap = new HashMap<>();
        EssValidateStoragePoolResponseMessage invalidValidateStoragePoolsResponse = new EssValidateStoragePoolResponseMessage();
        invalidValidateStoragePoolsResponse.setDeviceToStoragePoolMap(emptyDeviceToStoragePoolMap);
        List<Error> errors = new ArrayList<>();
        errors.add(new Error());
        invalidValidateStoragePoolsResponse.setErrors(errors);
        return invalidValidateStoragePoolsResponse;
    }

    private EssValidateStoragePoolResponseMessage createValidStoragePoolResponse()
    {
        Map<String, DeviceAssignment> deviceToStoragePoolMap = new HashMap<>();
        deviceToStoragePoolMap.put("jack", new DeviceAssignment("id1", "ser1", "lname1", "dname", "spid", "spname"));
        EssValidateStoragePoolResponseMessage validateStoragePoolsResponse = new EssValidateStoragePoolResponseMessage();
        validateStoragePoolsResponse.setDeviceToStoragePoolMap(deviceToStoragePoolMap);
        return validateStoragePoolsResponse;
    }

    private void mockFoundValidStoragePoolResponse() throws ServiceTimeoutException, ServiceExecutionException
    {
        doReturn(createValidStoragePoolResponse()).when(nodeService)
                .validateStoragePools(Mockito.anyList(), Mockito.anyList(), Mockito.anyMap());
    }

    private void setupMockScaleIODataList()
    {
        List<ScaleIOData> scaleIODataList = new ArrayList<>();
        ScaleIOData scaleIOData = new ScaleIOData("sio1", "name1", "installId", "mdmMode", "systemVersion", "clusterState", "version");

        ScaleIOProtectionDomain protectionDomain1 = new ScaleIOProtectionDomain("pdId1", "pdName1", "ACTIVE");
        scaleIOData.addProtectionDomain(protectionDomain1);
        protectionDomain1.setScaleIOData(scaleIOData);
        scaleIODataList.add(scaleIOData);
        doReturn(scaleIODataList).when(nodeService).listScaleIOData();
    }

    private void mockPopulatedNewDevices()
    {
        List<Device> mockDevices = new ArrayList<>();
        Device device = new Device("dev1Id", "dev1Name", "active", "dev1Serial", "dev1CanName", "dev1LogicalName", Device.Type.SSD,
                "900000000");
        mockDevices.add(device);
        doReturn(mockDevices).when(selectStoragePools).getNewDevices(anyString());
    }

    private void mockUnpopulatedNewDevices()
    {
        List<Device> mockDevices = new ArrayList<>();
        doReturn(mockDevices).when(selectStoragePools).getNewDevices(anyString());
    }
}