/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 **/

package com.dell.cpsd.paqx.dne.service.delegates.request;

import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.delegates.BaseWorkflowDelegate;
import com.dell.cpsd.paqx.dne.service.delegates.model.DelegateRequestModel;
import com.dell.cpsd.paqx.dne.transformers.HostPowerOperationsTransformer;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.PowerOperationRequest;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.REBOOT_HOST_FAILED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.REBOOT_HOST_MESSAGE_ID;

/**
 * Send reboot host request.
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @version 1.0
 * @since 1.0
 */
@Component
@Scope("prototype")
@Qualifier("sendRebootHostRequest")
public class SendRebootHostRequest extends BaseWorkflowDelegate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SendRebootHostRequest.class);

    private static final String REBOOT_HOST_ACTIVITY_ID = "receiveRebootHostResponse";

    private final AsynchronousNodeService        asynchronousNodeService;
    private final HostPowerOperationsTransformer hostPowerOperationsRequestTransformer;

    public SendRebootHostRequest(final AsynchronousNodeService asynchronousNodeService,
            final HostPowerOperationsTransformer hostPowerOperationsRequestTransformer)
    {
        this.asynchronousNodeService = asynchronousNodeService;
        this.hostPowerOperationsRequestTransformer = hostPowerOperationsRequestTransformer;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        LOGGER.info("Execute send reboot host task");
        final String taskMessage = "Send reboot host task";

        try
        {
            final DelegateRequestModel<HostPowerOperationRequestMessage> delegateRequestModel = hostPowerOperationsRequestTransformer
                    .buildHostPowerOperationsRequestMessage(delegateExecution, PowerOperationRequest.PowerOperation.REBOOT);

            final AsynchronousNodeServiceCallback<?> requestCallback = this.asynchronousNodeService
                    .sendRebootHostRequest(delegateExecution.getProcessInstanceId(), REBOOT_HOST_ACTIVITY_ID, REBOOT_HOST_MESSAGE_ID,
                            delegateRequestModel.getRequestMessage());

            if (requestCallback == null)
            {
                throw new IllegalStateException("Request callback is null");
            }

            final String returnMessage = taskMessage + " on Node " + delegateRequestModel.getServiceTag() + " was successful.";
            LOGGER.info(returnMessage);
            updateDelegateStatus(returnMessage);
        }
        catch (Exception ex)
        {
            String errorMessage = "An Unexpected Exception occurred attempting to request " + taskMessage + ". Reason: ";
            LOGGER.error(errorMessage, ex);
            updateDelegateStatus(errorMessage + ex.getMessage());
            throw new BpmnError(REBOOT_HOST_FAILED, errorMessage + ex.getMessage());
        }
    }
}
