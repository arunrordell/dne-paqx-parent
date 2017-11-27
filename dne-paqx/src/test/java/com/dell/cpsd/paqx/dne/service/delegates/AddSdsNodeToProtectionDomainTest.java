/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.exception.TaskResponseFailureException;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds;
import com.dell.cpsd.service.engineering.standards.DeviceAssignment;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AddSdsNodeToProtectionDomainTest
{
    @Mock
    private NodeService nodeService;

    @Mock
    private DataServiceRepository repository;

    @Mock
    private DelegateExecution delegateExecution;

    private AddSdsNodeToProtectionDomain addSdsNodeToProtectionDomain;
    private NodeDetail                   nodeDetail;
    private DeviceAssignment             deviceAssignment;
    private ComponentEndpointIds         componentEndpointIds;

    @Before
    public void setUp() throws Exception
    {
        addSdsNodeToProtectionDomain = new AddSdsNodeToProtectionDomain(nodeService, repository);

        deviceAssignment = new DeviceAssignment();
        deviceAssignment.setDeviceId("device-id");
        deviceAssignment.setDeviceName("device-name");
        deviceAssignment.setLogicalName("logical-name");
        deviceAssignment.setSerialNumber("serial-number");
        deviceAssignment.setStoragePoolId("storage-pool-id");
        deviceAssignment.setStoragePoolName("storage-pool-name");

        Map<String, DeviceAssignment> deviceAssignmentMap = new HashMap<>();
        deviceAssignmentMap.put("device-1", deviceAssignment);

        nodeDetail = new NodeDetail();
        nodeDetail.setServiceTag("abc");
        nodeDetail.setProtectionDomainId("protection-domain-id-1");
        nodeDetail.setDeviceToDeviceStoragePool(deviceAssignmentMap);
        nodeDetail.setEsxiManagementHostname("host-1");
        nodeDetail.setEsxiManagementIpAddress("1.2.3.4");
        nodeDetail.setScaleIoData1SvmIpAddress("1.2.3.5");
        nodeDetail.setScaleIoData2SvmIpAddress("1.2.3.6");

        componentEndpointIds = new ComponentEndpointIds("componentUuid", "endpointUuid", "endpointUrl", "credentialUuid");
    }

    @Test
    public void testTaskResponseFailureException() throws Exception
    {
        final String exceptionMsg = "request failed";
        when(delegateExecution.getVariable(NODE_DETAIL)).thenReturn(nodeDetail);
        when(repository.getDomainName()).thenReturn("example.com");
        when(repository.getComponentEndpointIds(anyString())).thenReturn(componentEndpointIds);
        willThrow(new TaskResponseFailureException(1, exceptionMsg)).given(nodeService).requestAddHostToProtectionDomain(any());

        try
        {
            addSdsNodeToProtectionDomain.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.ADD_VCENTER_HOST_TO_PROTECTION_DOMAIN));
            assertThat(error.getMessage(), containsString(exceptionMsg));
        }
    }

    @Test
    public void testGeneralException() throws Exception
    {
        when(delegateExecution.getVariable(NODE_DETAIL)).thenReturn(nodeDetail);
        when(repository.getDomainName()).thenReturn("example.com");
        when(repository.getComponentEndpointIds(anyString())).thenReturn(componentEndpointIds);
        willThrow(new NullPointerException()).given(nodeService).requestAddHostToProtectionDomain(any());

        try
        {
            addSdsNodeToProtectionDomain.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.ADD_VCENTER_HOST_TO_PROTECTION_DOMAIN));
            assertThat(error.getMessage(), containsString("An Unexpected Exception occurred"));
        }
    }

    @Test
    public void testSuccess() throws Exception
    {
        when(delegateExecution.getVariable(NODE_DETAIL)).thenReturn(nodeDetail);
        when(repository.getDomainName()).thenReturn("example.com");
        when(repository.getComponentEndpointIds(anyString())).thenReturn(componentEndpointIds);
        AddSdsNodeToProtectionDomain addSdsNodeToProtectionDomainSpy = spy(addSdsNodeToProtectionDomain);

        addSdsNodeToProtectionDomainSpy.delegateExecute(delegateExecution);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(addSdsNodeToProtectionDomainSpy).updateDelegateStatus(captor.capture());
        assertThat(captor.getValue(), CoreMatchers.containsString("was successful"));
    }
}