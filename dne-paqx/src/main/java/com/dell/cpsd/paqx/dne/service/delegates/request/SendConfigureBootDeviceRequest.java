/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates.request;

import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.delegates.BaseWorkflowDelegate;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.model.ConfigureBootDeviceIdracRequest;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.CONFIGURE_BOOT_DEVICE_MESSAGE_ID;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.SEND_CONFIGURE_BOOT_DEVICE_FAILED;

@Component
@Scope("prototype")
@Qualifier("sendConfigureBootDeviceRequest")
public class SendConfigureBootDeviceRequest extends BaseWorkflowDelegate
{
    /**
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SendConfigureBootDeviceRequest.class);

    /**
     * The <code>NodeService</code> instance
     */
    private AsynchronousNodeService asynchronousNodeService;

    @Autowired
    public SendConfigureBootDeviceRequest(AsynchronousNodeService asynchronousNodeService)
    {
        this.asynchronousNodeService = asynchronousNodeService;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        LOGGER.info("Execute Configure Boot Device");
        final String taskMessage = "The Request for Boot Device Configuration";
        NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);

        AsynchronousNodeServiceCallback<?> requestCallback = null;
        String uuid = nodeDetail.getId();
        String ipAddress = nodeDetail.getIdracIpAddress();

        ConfigureBootDeviceIdracRequest configureBootDeviceIdracRequest = new ConfigureBootDeviceIdracRequest();
        configureBootDeviceIdracRequest.setUuid(uuid);
        configureBootDeviceIdracRequest.setIdracIpAddress(ipAddress);

        requestCallback = asynchronousNodeService.bootDeviceIdracStatusRequest(delegateExecution.getProcessInstanceId(),
                                                                               "receiveConfigureBootDeviceResponse",
                                                                               CONFIGURE_BOOT_DEVICE_MESSAGE_ID,
                                                                               configureBootDeviceIdracRequest);
        if (requestCallback != null)
        {
            LOGGER.info(taskMessage + " was successful on Node " + nodeDetail.getServiceTag());
            updateDelegateStatus(taskMessage + " was successful on Node " + nodeDetail.getServiceTag());
        }
        else
        {
            LOGGER.error("Failed to send the request for Configure Boot Device on Node " + nodeDetail.getServiceTag());
            updateDelegateStatus(
                    "Failed to send the request for Configure Boot Device on Node " + nodeDetail.getServiceTag());
            throw new BpmnError(SEND_CONFIGURE_BOOT_DEVICE_FAILED,
                                "Failed to send the request for to Configure Boot Device on Node " +
                                nodeDetail.getServiceTag());
        }
    }
}
