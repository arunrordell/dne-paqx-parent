/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 **/

package com.dell.cpsd.paqx.dne.service.delegates.request;

import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.DelegateRequestModel;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import com.dell.cpsd.paqx.dne.transformers.HostPowerOperationsTransformer;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.PowerOperationRequest;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for send reboot host request
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @version 1.0
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class SendRebootHostRequestTest
{
    @Mock
    private AsynchronousNodeService asynchronousNodeService;

    @Mock
    private HostPowerOperationsTransformer hostPowerOperationsRequestTransformer;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private DelegateRequestModel<HostPowerOperationRequestMessage> requestModel;

    private SendRebootHostRequest            sendRebootHostRequest;
    private HostPowerOperationRequestMessage requestMessage;

    @Before
    public void setup() throws Exception
    {
        sendRebootHostRequest = new SendRebootHostRequest(asynchronousNodeService, hostPowerOperationsRequestTransformer);
        requestMessage = new HostPowerOperationRequestMessage();
    }

    @Test
    public void exceptionThrownWhileBuildingRequestMessageThrowsBpmnError() throws Exception
    {
        final String errorMessage = "Something bad happened";
        when(hostPowerOperationsRequestTransformer
                .buildHostPowerOperationsRequestMessage(delegateExecution, PowerOperationRequest.PowerOperation.REBOOT))
                .thenThrow(new IllegalStateException(errorMessage));
        try
        {
            sendRebootHostRequest.delegateExecute(delegateExecution);
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.REBOOT_HOST_FAILED));
            assertThat(error.getMessage(), containsString("An Unexpected Exception occurred attempting to request"));
            assertThat(error.getMessage(), containsString(errorMessage));
        }
    }

    @Test
    public void exceptionThrownResultsInNullServiceCallBackResultsInBpmnError() throws Exception
    {
        when(hostPowerOperationsRequestTransformer
                .buildHostPowerOperationsRequestMessage(delegateExecution, PowerOperationRequest.PowerOperation.REBOOT))
                .thenReturn(requestModel);
        try
        {
            sendRebootHostRequest.delegateExecute(delegateExecution);
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.REBOOT_HOST_FAILED));
            assertThat(error.getMessage(), containsString("An Unexpected Exception occurred attempting to request"));
            assertThat(error.getMessage(), containsString("Request callback is null"));
        }
    }

    @Test
    public void rebootHostSuccessful() throws Exception
    {
        final AsynchronousNodeServiceCallback mockRequestCallback = mock(AsynchronousNodeServiceCallback.class);
        when(requestModel.getRequestMessage()).thenReturn(requestMessage);
        when(delegateExecution.getProcessInstanceId()).thenReturn("123");
        when(hostPowerOperationsRequestTransformer
                .buildHostPowerOperationsRequestMessage(delegateExecution, PowerOperationRequest.PowerOperation.REBOOT))
                .thenReturn(requestModel);
        doReturn(mockRequestCallback).when(asynchronousNodeService)
                .sendRebootHostRequest("123", "receiveRebootHostResponse", "rebootHostResponseReceived", requestMessage);

        final SendRebootHostRequest rebootHostSpy = spy(sendRebootHostRequest);

        rebootHostSpy.delegateExecute(delegateExecution);

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(rebootHostSpy).updateDelegateStatus(captor.capture());
        assertThat(captor.getValue(), containsString("was successful"));
        assertThat(captor.getValue(), containsString("Send reboot host task"));
    }
}
