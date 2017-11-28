/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.amqp.adapter;

import com.dell.cpsd.InstallESXiResponseMessage;
import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.service.common.client.callback.IServiceCallback;
import com.dell.cpsd.service.common.client.callback.ServiceResponse;
import com.dell.cpsd.service.common.client.rpc.ServiceCallbackAdapter;
import com.dell.cpsd.service.common.client.rpc.ServiceCallbackRegistry;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;

/**
 * TODO: Document Usage
 *
 * <p/>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * <p/>
 *
 * @version 1.0
 * @since 1.0
 */
public class InstallEsxiResponseAdapter
        implements ServiceCallbackAdapter<InstallESXiResponseMessage, ServiceResponse<InstallESXiResponseMessage>>
{
    private final ServiceCallbackRegistry serviceCallbackRegistry;
    private final RuntimeService runtimeService;

    public InstallEsxiResponseAdapter(final ServiceCallbackRegistry serviceCallbackRegistry, final RuntimeService runtimeService)
    {
        this.serviceCallbackRegistry = serviceCallbackRegistry;
        this.runtimeService = runtimeService;
    }

    @Override
    public ServiceResponse<InstallESXiResponseMessage> transform(final InstallESXiResponseMessage responseMessage)
    {
        return new ServiceResponse<>(responseMessage.getMessageProperties().getCorrelationId(), responseMessage, null);
    }

    @Override
    public void consume(final IServiceCallback callback, final ServiceResponse<InstallESXiResponseMessage> responseMessage)
    {
        callback.handleServiceResponse(responseMessage);
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
    public IServiceCallback take(final InstallESXiResponseMessage installESXiResponseMessage)
    {
        return serviceCallbackRegistry.removeServiceCallback(installESXiResponseMessage.getMessageProperties().getCorrelationId());
    }

    @Override
    public Class<InstallESXiResponseMessage> getSourceClass()
    {
        return InstallESXiResponseMessage.class;
    }
}