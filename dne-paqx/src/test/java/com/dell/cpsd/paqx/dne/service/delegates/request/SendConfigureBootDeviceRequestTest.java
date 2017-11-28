/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.delegates.request;

import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import com.dell.cpsd.service.common.client.callback.ServiceResponse;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SendConfigureBootDeviceRequestTest
{
    private SendConfigureBootDeviceRequest                      sendConfigureBootDeviceRequest;
    private AsynchronousNodeService                             asynchronousNodeService;
    private DelegateExecution                                   delegateExecution;
    private NodeDetail                                          nodeDetail;
    private AsynchronousNodeServiceCallback<ServiceResponse<?>> asynchronousNodeServiceCallback;

    @Before
    public void setUp() throws Exception
    {
        asynchronousNodeService = mock(AsynchronousNodeService.class);

        sendConfigureBootDeviceRequest = new SendConfigureBootDeviceRequest(asynchronousNodeService);

        asynchronousNodeServiceCallback = mock(AsynchronousNodeServiceCallback.class);
        doReturn(asynchronousNodeServiceCallback).when(asynchronousNodeService).bootDeviceIdracStatusRequest(any(), any(), any(), any());

        delegateExecution = mock(DelegateExecution.class);

        nodeDetail = new NodeDetail();
        nodeDetail.setId("1");
        nodeDetail.setIdracIpAddress("1");
        nodeDetail.setIdracGatewayIpAddress("1");
        nodeDetail.setIdracSubnetMask("1");
        nodeDetail.setServiceTag("abc");

        doReturn(nodeDetail).when(delegateExecution).getVariable(NODE_DETAIL);
        doReturn("1").when(delegateExecution).getProcessInstanceId();
    }

    @Test
    public void delegateExecuteSuccess() throws Exception
    {
        SendConfigureBootDeviceRequest sendConfigureBootDeviceRequestSpy = spy(sendConfigureBootDeviceRequest);

        sendConfigureBootDeviceRequestSpy.delegateExecute(delegateExecution);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sendConfigureBootDeviceRequestSpy).updateDelegateStatus(captor.capture());
        assertThat(captor.getValue(), containsString("was successful"));
    }

    @Test
    public void delegateExecuteRequestUnsuccessful() throws Exception
    {

        doReturn(null).when(asynchronousNodeService).bootDeviceIdracStatusRequest(any(), any(), any(), any());
        try
        {
            sendConfigureBootDeviceRequest.delegateExecute(delegateExecution);
            fail("An exception was expected.");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.SEND_CONFIGURE_BOOT_DEVICE_FAILED));
            assertTrue(error.getMessage().equals("Failed to send the request for to Configure Boot Device on Node abc"));
        }

    }
}
