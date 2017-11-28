/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.delegates.request;

import com.dell.cpsd.EsxiInstallationInfo;
import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import com.dell.cpsd.paqx.dne.transformers.HostToInstallEsxiRequestTransformer;
import com.dell.cpsd.service.common.client.callback.ServiceResponse;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SendInstallEsxiRequestTest
{
    private SendInstallEsxiRequest sendInstallEsxiRequest;

    @Mock
    private AsynchronousNodeService asynchronousNodeService;

    @Mock
    private HostToInstallEsxiRequestTransformer hostToInstallEsxiRequestTransformer;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private EsxiInstallationInfo esxiInstallationInfo;

    @Mock
    private AsynchronousNodeServiceCallback<ServiceResponse<?>> asynchronousNodeServiceCallback;

    private NodeDetail nodeDetail;

    @Before
    public void setUp() throws Exception
    {
        sendInstallEsxiRequest = new SendInstallEsxiRequest(asynchronousNodeService, hostToInstallEsxiRequestTransformer);

        doReturn(this.esxiInstallationInfo).when(hostToInstallEsxiRequestTransformer).transformInstallEsxiData(any(), any(), any());

        doReturn(asynchronousNodeServiceCallback).when(asynchronousNodeService).requestInstallEsxi(any(), any(), any(), any());

        nodeDetail = new NodeDetail();
        nodeDetail.setId("1");
        nodeDetail.setIdracIpAddress("1");
        nodeDetail.setIdracGatewayIpAddress("1");
        nodeDetail.setIdracSubnetMask("1");
        nodeDetail.setServiceTag("abc");
        nodeDetail.setEsxiManagementHostname("hostName");

        doReturn(nodeDetail).when(delegateExecution).getVariable(NODE_DETAIL);
        doReturn("1").when(delegateExecution).getProcessInstanceId();
    }

    @Test
    public void delegateExecuteSuccess() throws Exception
    {
        SendInstallEsxiRequest sendInstallEsxiRequestSpy = spy(sendInstallEsxiRequest);

        sendInstallEsxiRequestSpy.delegateExecute(delegateExecution);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sendInstallEsxiRequestSpy).updateDelegateStatus(captor.capture());
        assertThat(captor.getValue(), containsString("was successful"));
    }

    @Test
    public void delegateExecuteRequestUnsuccessful() throws Exception
    {

        doReturn(null).when(asynchronousNodeService).requestInstallEsxi(any(), any(), any(), any());
        try
        {
            sendInstallEsxiRequest.delegateExecute(delegateExecution);
            fail("An exception was expected.");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.SEND_INSTALL_ESXI_FAILED));
            assertThat(error.getMessage(), containsString("Failed to send the request"));
        }
    }
}
