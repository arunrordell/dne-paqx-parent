/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.amqp.adapter;

import com.dell.cpsd.ConfigureBootDeviceIdracResponseMessage;
import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.service.common.client.callback.IServiceCallback;
import com.dell.cpsd.service.common.client.callback.ServiceResponse;
import com.dell.cpsd.service.common.client.rpc.ServiceCallbackAdapter;
import com.dell.cpsd.service.common.client.rpc.ServiceCallbackRegistry;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;

/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */
public class ConfigureBootDeviceIdracResponseAdapter
        implements ServiceCallbackAdapter<ConfigureBootDeviceIdracResponseMessage, ServiceResponse<ConfigureBootDeviceIdracResponseMessage>>
{
    private ServiceCallbackRegistry serviceCallbackRegistry;
    private RuntimeService runtimeService;

    public ConfigureBootDeviceIdracResponseAdapter(ServiceCallbackRegistry serviceCallbackRegistry, RuntimeService runtimeService)
    {
        this.serviceCallbackRegistry = serviceCallbackRegistry;
        this.runtimeService = runtimeService;
    }

    @Override
    public ServiceResponse<ConfigureBootDeviceIdracResponseMessage> transform(
            ConfigureBootDeviceIdracResponseMessage configureBootDeviceIdracResponseMessage)
    {
        return new ServiceResponse<>(configureBootDeviceIdracResponseMessage.getMessageProperties().getCorrelationId(),
                configureBootDeviceIdracResponseMessage, null);
    }

    @Override
    public void consume(IServiceCallback callback,
                        ServiceResponse<ConfigureBootDeviceIdracResponseMessage> configureBootDeviceIdracResponseMessage)
    {
        callback.handleServiceResponse(configureBootDeviceIdracResponseMessage);
        if (runtimeService != null && callback instanceof AsynchronousNodeServiceCallback)
        {
            AsynchronousNodeServiceCallback<?> async = (AsynchronousNodeServiceCallback<?>) callback;
            if (async.getProcessInstanceId() != null)
            {
                Execution execution = runtimeService.createExecutionQuery().processInstanceId(
                        async.getProcessInstanceId()).activityId(async.getActivityId()).singleResult();
                if (execution != null)
                {
                    runtimeService.setVariable(execution.getId(), async.getMessageId(), callback);
                    runtimeService.messageEventReceived(async.getMessageId(), execution.getId());
                }
            }
        }
    }

    @Override
    public IServiceCallback take(ConfigureBootDeviceIdracResponseMessage configureBootDeviceIdracResponseMessage)
    {
        return serviceCallbackRegistry
                .removeServiceCallback(configureBootDeviceIdracResponseMessage.getMessageProperties().getCorrelationId());
    }

    @Override
    public Class<ConfigureBootDeviceIdracResponseMessage> getSourceClass()
    {
        return ConfigureBootDeviceIdracResponseMessage.class;
    }

    public RuntimeService getRuntimeService()
    {
        return runtimeService;
    }

    public void setRuntimeService(final RuntimeService runtimeService)
    {
        this.runtimeService = runtimeService;
    }
}
