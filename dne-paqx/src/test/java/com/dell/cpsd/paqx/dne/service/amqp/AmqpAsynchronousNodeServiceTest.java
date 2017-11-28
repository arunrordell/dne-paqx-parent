/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */
package com.dell.cpsd.paqx.dne.service.amqp;

import com.dell.cpsd.ConfigureBootDeviceIdracResponseMessage;
import com.dell.cpsd.MessageProperties;
import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.amqp.producer.DneProducer;
import com.dell.cpsd.paqx.dne.exception.TaskResponseFailureException;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.model.BootDeviceIdracStatus;
import com.dell.cpsd.paqx.dne.service.model.ConfigureBootDeviceIdracRequest;
import com.dell.cpsd.service.common.client.callback.ServiceError;
import com.dell.cpsd.service.common.client.callback.ServiceResponse;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import com.dell.cpsd.service.common.client.rpc.DefaultMessageConsumer;
import com.dell.cpsd.service.common.client.rpc.DelegatingMessageConsumer;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationResponseMessage;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AmqpAsynchronousNodeServiceTest
{
    private DelegatingMessageConsumer consumer;
    private DneProducer dneProducer;
    private DataServiceRepository repository;
    private RuntimeService runtimeService;
    private String replyTo;

    private static final String processInstanceId = "PI1";
    private static final String activityId = "Activity1";
    private static final String messageId = "Message1";

    private AmqpAsynchronousNodeService amqpAsynchronousNodeService;

    @Before
    public void setup() {
        consumer = mock(DefaultMessageConsumer.class);
        dneProducer = mock(DneProducer.class);
        repository = mock(DataServiceRepository.class);
        runtimeService = mock(RuntimeService.class);
        replyTo = "AmqpAsynchronousNodeServiceTest";

        amqpAsynchronousNodeService = new AmqpAsynchronousNodeService(consumer,
                                                                      dneProducer,
                                                                      replyTo,
                                                                      repository,
                                                                      runtimeService);

    }

    @Ignore
    public void verifyInitialization() {
        verify(consumer, times(1)).addAdapter(any());
    }

    @Test
    public void bootDeviceIdracStatusRequestSuccessful() throws Exception
    {
        ConfigureBootDeviceIdracRequest request =  new ConfigureBootDeviceIdracRequest();
        request.setUuid("UUID1");
        request.setIdracIpAddress("192.168.1.1");

        AsynchronousNodeServiceCallback<?> callback = amqpAsynchronousNodeService.bootDeviceIdracStatusRequest(processInstanceId,
                                                                                                               activityId,
                                                                                                               messageId,
                                                                                                               request);
        assertNotNull(callback);
        assertEquals(callback.getProcessInstanceId(), processInstanceId);
        assertEquals(callback.getActivityId(), activityId);
        assertEquals(callback.getMessageId(), messageId);
        verify(dneProducer, times(1)).publishConfigureBootDeviceIdrac(any());
    }

    @Test
    public void bootDeviceIdracStatusRequestException() throws Exception
    {
        ConfigureBootDeviceIdracRequest request =  new ConfigureBootDeviceIdracRequest();
        request.setUuid("UUID1");
        request.setIdracIpAddress("192.168.1.1");

        amqpAsynchronousNodeService.release();

        AsynchronousNodeServiceCallback<?> callback = amqpAsynchronousNodeService.bootDeviceIdracStatusRequest(processInstanceId,
                                                                                                               activityId,
                                                                                                               messageId,
                                                                                                               request);
        assertNull(callback);
    }

    @Test
    public void bootDeviceIdracStatusResponseSuccessful() throws Exception
    {
        AsynchronousNodeServiceCallback<ServiceResponse<ConfigureBootDeviceIdracResponseMessage>> callback = mock(AsynchronousNodeServiceCallback.class);
        when(callback.isDone()).thenReturn(true);
        when(callback.getProcessInstanceId()).thenReturn(processInstanceId);
        when(callback.getActivityId()).thenReturn(activityId);
        when(callback.getMessageId()).thenReturn(messageId);
        when(callback.getServiceError()).thenReturn(null);

        ConfigureBootDeviceIdracResponseMessage responseMessage = new ConfigureBootDeviceIdracResponseMessage();
        MessageProperties messageProperties = mock(MessageProperties.class);
        responseMessage.setMessageProperties(messageProperties);
        responseMessage.setStatus(ConfigureBootDeviceIdracResponseMessage.Status.SUCCESS);
        ServiceResponse<ConfigureBootDeviceIdracResponseMessage> response = new ServiceResponse<ConfigureBootDeviceIdracResponseMessage>("UUID1", responseMessage, "Configure");
        when(callback.getServiceResponse()).thenReturn(response);

        BootDeviceIdracStatus status = amqpAsynchronousNodeService.bootDeviceIdracStatusResponse(callback);

        assertNotNull(status);
        assertEquals(status.getStatus(), "SUCCESS");
    }

    @Test(expected = ServiceExecutionException.class)
    public void bootDeviceIdracStatusResponseException() throws Exception
    {
        AsynchronousNodeServiceCallback<ServiceResponse<ConfigureBootDeviceIdracResponseMessage>> callback = mock(
                AsynchronousNodeServiceCallback.class);
        when(callback.isDone()).thenReturn(true);
        when(callback.getProcessInstanceId()).thenReturn(processInstanceId);
        when(callback.getActivityId()).thenReturn(activityId);
        when(callback.getMessageId()).thenReturn(messageId);
        ServiceError error = new ServiceError("UUID1", "error1", "ErrorMessage1");
        when(callback.getServiceError()).thenReturn(error);

        amqpAsynchronousNodeService.bootDeviceIdracStatusResponse(callback);
    }

    @Test
    public void sendRebootHostRequestExceptionThrownReturnsNullResponse() throws Exception
    {
        final HostPowerOperationRequestMessage request = new HostPowerOperationRequestMessage();

        amqpAsynchronousNodeService.release();

        final AsynchronousNodeServiceCallback<?> callback = amqpAsynchronousNodeService
                .sendRebootHostRequest(processInstanceId, activityId, messageId, request);

        assertNull(callback);
        verify(dneProducer, times(0)).publishRebootHost(any());
    }

    @Test
    public void sendRebootHostRequestSuccess() throws Exception
    {
        final HostPowerOperationRequestMessage request = new HostPowerOperationRequestMessage();

        final AsynchronousNodeServiceCallback<?> callback = amqpAsynchronousNodeService
                .sendRebootHostRequest(processInstanceId, activityId, messageId, request);
        assertNotNull(callback);
        assertEquals(callback.getProcessInstanceId(), processInstanceId);
        assertEquals(callback.getActivityId(), activityId);
        assertEquals(callback.getMessageId(), messageId);
        verify(dneProducer, times(1)).publishRebootHost(any());
    }

    @Test
    public void processRebootHostResponseCallbackIsNullExceptionThrown() throws Exception
    {
        try
        {
            amqpAsynchronousNodeService.processRebootHostResponse(null);
        }
        catch (TaskResponseFailureException e)
        {
            assertTrue(e.getCode() == 1012);
            assertThat(e.getMessage(), containsString("Service callback is null"));
        }
    }

    @Test
    public void processRebootHostResponseMessageIsNullExceptionThrown() throws Exception
    {
        final AsynchronousNodeServiceCallback<ServiceResponse<HostPowerOperationResponseMessage>> callback = mock(
                AsynchronousNodeServiceCallback.class);
        try
        {
            amqpAsynchronousNodeService.processRebootHostResponse(callback);
        }
        catch (TaskResponseFailureException e)
        {
            assertTrue(e.getCode() == 1012);
            assertThat(e.getMessage(), containsString("Response message is null"));
        }
    }

    @Test
    public void processRebootHostResponseFailedThrowsException() throws Exception
    {
        final AsynchronousNodeServiceCallback<ServiceResponse<HostPowerOperationResponseMessage>> callback = mock(
                AsynchronousNodeServiceCallback.class);

        final HostPowerOperationResponseMessage responseMessage = new HostPowerOperationResponseMessage();
        final com.dell.cpsd.virtualization.capabilities.api.MessageProperties messageProperties = mock(com.dell.cpsd.virtualization.capabilities.api.MessageProperties.class);
        responseMessage.setMessageProperties(messageProperties);
        responseMessage.setDescription("VCenter validation failed");
        responseMessage.setStatus(HostPowerOperationResponseMessage.Status.FAILED);
        ServiceResponse<HostPowerOperationResponseMessage> response = new ServiceResponse<>("UUID1", responseMessage, "Configure");
        when(callback.getServiceResponse()).thenReturn(response);

        try
        {
            amqpAsynchronousNodeService.processRebootHostResponse(callback);
        }
        catch (TaskResponseFailureException e)
        {
            assertTrue(e.getCode() == 1012);
            assertThat(e.getMessage(), containsString("VCenter validation failed"));
        }
    }
}
