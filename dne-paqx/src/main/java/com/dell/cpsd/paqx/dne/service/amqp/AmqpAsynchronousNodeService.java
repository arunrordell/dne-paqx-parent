/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */
package com.dell.cpsd.paqx.dne.service.amqp;

import com.dell.cpsd.ConfigureBootDeviceIdracError;
import com.dell.cpsd.ConfigureBootDeviceIdracRequestMessage;
import com.dell.cpsd.ConfigureBootDeviceIdracResponseMessage;
import com.dell.cpsd.EsxiInstallationInfo;
import com.dell.cpsd.InstallESXiRequestMessage;
import com.dell.cpsd.InstallESXiResponseMessage;
import com.dell.cpsd.common.logging.ILogger;
import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.amqp.config.AsynchronousNodeServiceConfig;
import com.dell.cpsd.paqx.dne.amqp.producer.DneProducer;
import com.dell.cpsd.paqx.dne.exception.TaskResponseFailureException;
import com.dell.cpsd.paqx.dne.log.DneLoggingManager;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.amqp.adapter.ConfigureBootDeviceIdracResponseAdapter;
import com.dell.cpsd.paqx.dne.service.amqp.adapter.InstallEsxiResponseAdapter;
import com.dell.cpsd.paqx.dne.service.amqp.adapter.RebootHostResponseAdapter;
import com.dell.cpsd.paqx.dne.service.model.BootDeviceIdracStatus;
import com.dell.cpsd.paqx.dne.service.model.ConfigureBootDeviceIdracRequest;
import com.dell.cpsd.service.common.client.callback.IServiceCallback;
import com.dell.cpsd.service.common.client.callback.ServiceError;
import com.dell.cpsd.service.common.client.callback.ServiceResponse;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import com.dell.cpsd.service.common.client.log.SCCLMessageCode;
import com.dell.cpsd.service.common.client.manager.AbstractServiceCallbackManager;
import com.dell.cpsd.service.common.client.rpc.DelegatingMessageConsumer;
import com.dell.cpsd.service.common.client.rpc.ServiceCallbackRegistry;
import com.dell.cpsd.service.common.client.rpc.ServiceRequestCallback;
import com.dell.cpsd.service.common.client.task.ServiceTask;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationResponseMessage;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.util.CollectionUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.REBOOT_HOST;

public class AmqpAsynchronousNodeService extends AbstractServiceCallbackManager implements ServiceCallbackRegistry,
        AsynchronousNodeService
{
    /*
     * The logger instance
     */
    private static ILogger LOGGER = DneLoggingManager.getLogger(AsynchronousNodeServiceConfig.class);

    /*
     * The <code>DelegatingMessageConsumer</code>
     */
    private final DelegatingMessageConsumer consumer;

    /*
     * The <code>DneProducer</code>
     */
    private final DneProducer producer;

    /*
     * The replyTo queue name
     */
    private final String replyTo;

    /*
     * The <code>DataServiceRepository</code>
     */
    private final DataServiceRepository repository;

    /**
     * The Camunda Runtime Service
     */
    private RuntimeService runtimeService;


    /**
     * AmqpNodeService constructor.
     *
     * @param consumer                                - The <code>DelegatingMessageConsumer</code> instance.
     * @param producer                                - The <code>DneProducer</code> instance.
     * @param replyTo                                 - The replyTo queue name.
     * @param repository                              - The <code>DataServiceRepository</code> instance.
     * @param runtimeService                          - The <code>RuntimeService</code> instance.
     * @since 1.1
     */
    public AmqpAsynchronousNodeService(final DelegatingMessageConsumer consumer,
                                       final DneProducer producer,
                                       final String replyTo,
                                       final DataServiceRepository repository,
                                       final RuntimeService runtimeService)
    {
        this.consumer = consumer;
        this.producer = producer;
        this.replyTo = replyTo;
        this.repository = repository;
        this.runtimeService = runtimeService;
        initCallbacks();
    }

    /*
 * Initialize message consumer adapters.
 *
 * @since 1.0
 */
    private void initCallbacks()
    {
        this.consumer.addAdapter(new ConfigureBootDeviceIdracResponseAdapter(this, this.runtimeService));
        this.consumer.addAdapter(new InstallEsxiResponseAdapter(this, this.runtimeService));
        this.consumer.addAdapter(new RebootHostResponseAdapter(this, this.runtimeService));
    }

    /**
     *
     * @param activityId
     * @param messageId
     * @param timeout
     * @param serviceRequestCallback
     * @param <RES>
     * @return
     * @throws ServiceExecutionException
     */
    protected <RES extends ServiceResponse<?>> AsynchronousNodeServiceCallback<RES> processRequest(final String processInstanceId,
                                                                                                final String activityId,
                                                                                                final String messageId,
                                                                                                long timeout,
                                                                                                ServiceRequestCallback serviceRequestCallback) throws ServiceExecutionException
    {
        this.shutdownCheck();
        String requestId = serviceRequestCallback.getRequestId();
        if (requestId == null) {
            requestId = this.createRequestId();
        }

        AsynchronousNodeServiceCallback<RES> serviceCallback = new AsynchronousNodeServiceCallback(processInstanceId, activityId, messageId);
        this.createAndAddServiceTask(requestId, serviceCallback, timeout);

        try {
            serviceRequestCallback.executeRequest(requestId);
        } catch (Exception var7) {
            this.removeServiceTask(requestId);
            this.logAndThrowException(var7);
        }
        return serviceCallback;
    }

    /**
     * Process a RPC response message.
     *
     * @param responseCallback - The <code>AsynchronousServiceCallBack</code> to process.
     * @param expectedResponse - The expected response <code>Class</code>
     * @return The response.
     * @since 1.1
     */
    private <R> R processResponse(AsynchronousNodeServiceCallback<?> responseCallback, Class<R> expectedResponse)
            throws ServiceExecutionException
    {
        R response = null;
        if (responseCallback != null)
        {
            checkForServiceError(responseCallback);
            ServiceResponse<R> serviceResponse = (ServiceResponse<R>) responseCallback.getServiceResponse();
            if (serviceResponse != null)
            {
                Object responseMessage = serviceResponse.getResponse();
                if (responseMessage == null)
                {
                    return null;
                }

                if (expectedResponse.isAssignableFrom(responseMessage.getClass()))
                {
                    response = (R) responseMessage;
                }
                else
                {
                    throw new UnsupportedOperationException("Unexpected response message: " + responseMessage);
                }
            }
        }
        return response;
    }

    public void release() {
        super.release();
    }

    private void createAndAddServiceTask(String requestId, AsynchronousNodeServiceCallback<?> callback, long timeout) {
        ServiceTask<IServiceCallback<?>> task = new ServiceTask(requestId, callback, timeout);
        this.addServiceTask(requestId, task);
    }

    private void checkForServiceError(AsynchronousNodeServiceCallback<?> callback) throws ServiceExecutionException {
        ServiceError error = callback.getServiceError();
        if (error != null) {
            throw new ServiceExecutionException(error.getErrorMessage());
        }
    }

    private void logAndThrowException(Exception exception) throws ServiceExecutionException {
        Object[] lparams = new Object[]{exception.getMessage()};
        String lmessage = LOGGER.error(SCCLMessageCode.PUBLISH_MESSAGE_FAIL_E.getMessageCode(), lparams, exception);
        throw new ServiceExecutionException(lmessage, exception);
    }

    protected String createRequestId() {
        return this.uuid();
    }

    protected String uuid() {
        return UUID.randomUUID().toString();
    }

    private void shutdownCheck() throws ServiceExecutionException {
        if (this.isShutDown()) {
            String lmessage = LOGGER.error(SCCLMessageCode.MANAGER_SHUTDOWN_E.getMessageCode());
            throw new ServiceExecutionException(lmessage);
        }
    }

    @Override
    public <T extends ServiceResponse<?>> AsynchronousNodeServiceCallback<?> bootDeviceIdracStatusRequest(final String processId,
                                                                                                          final String activityId,
                                                                                                          final String messageId,
                                                                                                          final ConfigureBootDeviceIdracRequest configureBootDeviceIdracRequest)
    {
        AsynchronousNodeServiceCallback<?> response = null;
        if (configureBootDeviceIdracRequest != null &&
            StringUtils.isNotEmpty(configureBootDeviceIdracRequest.getUuid()) &&
            StringUtils.isNotEmpty(configureBootDeviceIdracRequest.getIdracIpAddress()))
        {
            try
            {
                ConfigureBootDeviceIdracRequestMessage configureBootDeviceIdracRequestMessage = new ConfigureBootDeviceIdracRequestMessage();

                com.dell.cpsd.MessageProperties messageProperties = new com.dell.cpsd.MessageProperties();
                messageProperties.setCorrelationId(UUID.randomUUID().toString());
                messageProperties.setTimestamp(Calendar.getInstance().getTime());
                messageProperties.setReplyTo(replyTo);
                configureBootDeviceIdracRequestMessage.setMessageProperties(messageProperties);

                configureBootDeviceIdracRequestMessage.setUuid(configureBootDeviceIdracRequest.getUuid());
                configureBootDeviceIdracRequestMessage.setIpAddress(configureBootDeviceIdracRequest.getIdracIpAddress());

                response = processRequest(processId, activityId, messageId, 0L, new ServiceRequestCallback()
                {
                    @Override
                    public String getRequestId()
                    {
                        return messageProperties.getCorrelationId();
                    }

                    @Override
                    public void executeRequest(String requestId) throws Exception
                    {
                        producer.publishConfigureBootDeviceIdrac(configureBootDeviceIdracRequestMessage);
                    }
                });
            }
            catch (Exception e)
            {
                LOGGER.error(" An unexpected exception occurred requesting Configure Boot Device on Node.", e);
            }
        }
        return response;
    }

    @Override
    public BootDeviceIdracStatus bootDeviceIdracStatusResponse(AsynchronousNodeServiceCallback<?> serviceCallback)
            throws ServiceExecutionException
    {
        BootDeviceIdracStatus bootDeviceIdracStatus = null;
        if (serviceCallback != null)
        {
            ConfigureBootDeviceIdracResponseMessage resp = processResponse(serviceCallback,
                                                                           ConfigureBootDeviceIdracResponseMessage.class);
            if (resp != null)
            {
                bootDeviceIdracStatus = new BootDeviceIdracStatus();
                if (resp.getMessageProperties() != null)
                {
                    if (resp.getStatus() != null)
                    {
                        LOGGER.info("Response message is: " + resp.getStatus().toString());

                        bootDeviceIdracStatus.setStatus(resp.getStatus().toString());
                        List<ConfigureBootDeviceIdracError> errors = resp.getConfigureBootDeviceIdracErrors();
                        if (!CollectionUtils.isEmpty(errors))
                        {
                            List<String> errorMsgs = errors.stream().map(ConfigureBootDeviceIdracError::getMessage).collect(Collectors.toList());
                            bootDeviceIdracStatus.setErrors(errorMsgs);
                        }
                    }
                }
            }
        }
        return bootDeviceIdracStatus;
    }

    @Override
    public <T extends ServiceResponse<?>> AsynchronousNodeServiceCallback<?> requestInstallEsxi(final String processId,
                                                                                                final String activityId,
                                                                                                final String messageId,
                                                                                                final EsxiInstallationInfo esxiInstallationInfo)
    {

        AsynchronousNodeServiceCallback<?> response = null;
        try
        {
            final InstallESXiRequestMessage requestMessage = new InstallESXiRequestMessage();
            final String correlationId = UUID.randomUUID().toString();
            requestMessage.setMessageProperties(new com.dell.cpsd.MessageProperties(new Date(), correlationId, replyTo));
            requestMessage.setEsxiInstallationInfo(esxiInstallationInfo);

            response = processRequest(processId, activityId, messageId, 0L, new ServiceRequestCallback()
            {
                @Override
                public String getRequestId()
                {
                    return correlationId;
                }

                @Override
                public void executeRequest(String requestId) throws Exception
                {
                    producer.publishInstallEsxiRequest(requestMessage);
                }
            });

        }
        catch (Exception e) {
            LOGGER.error(" An unexpected exception occurred requesting Installation of Esxi on Node.", e);
        }
        return response;
    }

    @Override
    public String requestInstallEsxi(final AsynchronousNodeServiceCallback<?> serviceCallback) throws ServiceExecutionException
    {
        String status = null;
        if (serviceCallback != null)
        {
            InstallESXiResponseMessage responseMessage = processResponse(serviceCallback, InstallESXiResponseMessage.class);
            if (responseMessage != null && responseMessage.getMessageProperties() != null)
            {
                status = responseMessage.getStatus();
            }
        }
        return status;
    }

    @Override
    public AsynchronousNodeServiceCallback<?> sendRebootHostRequest(final String processId, final String activityId, final String messageId,
            final HostPowerOperationRequestMessage requestMessage)
    {
        AsynchronousNodeServiceCallback<?> response = null;

        try
        {
            final String correlationId = UUID.randomUUID().toString();
            requestMessage.setMessageProperties(
                    new com.dell.cpsd.virtualization.capabilities.api.MessageProperties(new Date(), correlationId, replyTo));
            response = processRequest(processId, activityId, messageId, 0L, new ServiceRequestCallback()
            {
                @Override
                public String getRequestId()
                {
                    return correlationId;
                }

                @Override
                public void executeRequest(String requestId) throws Exception
                {
                    producer.publishRebootHost(requestMessage);
                }
            });
        }
        catch (Exception e)
        {
            LOGGER.error(" An unexpected exception occurred while requesting reboot host", e);
        }
        return response;
    }

    @Override
    public void processRebootHostResponse(final AsynchronousNodeServiceCallback<?> serviceCallback) throws TaskResponseFailureException
    {
        try
        {
            if (serviceCallback == null)
            {
                final String error = "Service callback is null";
                LOGGER.error(error);
                throw new TaskResponseFailureException(REBOOT_HOST.getCode(), error);
            }

            final HostPowerOperationResponseMessage responseMessage = processResponse(serviceCallback, HostPowerOperationResponseMessage.class);
            if (responseMessage == null)
            {
                final String error = "Response message is null";
                LOGGER.error(error);
                throw new TaskResponseFailureException(REBOOT_HOST.getCode(), error);
            }

            if (responseMessage.getStatus().equals(HostPowerOperationResponseMessage.Status.FAILED))
            {
                LOGGER.error(responseMessage.getDescription());
                throw new TaskResponseFailureException(REBOOT_HOST.getCode(), responseMessage.getDescription());
            }
        }
        catch (ServiceExecutionException e)
        {
            LOGGER.error("Exception occurred", e);
            throw new TaskResponseFailureException(REBOOT_HOST.getCode(), e.getMessage());
        }
    }
}
