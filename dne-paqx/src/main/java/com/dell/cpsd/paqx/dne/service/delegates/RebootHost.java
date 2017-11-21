/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds;
import com.dell.cpsd.virtualization.capabilities.api.Credentials;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.PowerOperationRequest;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.HOSTNAME;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.REBOOT_HOST_FAILED;

@Component
@Scope("prototype")
@Qualifier("rebootHost")
public class RebootHost extends BaseWorkflowDelegate
{
    /**
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RebootHost.class);

    /**
     * The <code>NodeService</code> instance
     */
    private final NodeService           nodeService;
    private final DataServiceRepository repository;

    @Autowired
    public RebootHost(final NodeService nodeService, final DataServiceRepository repository)
    {
        this.nodeService = nodeService;
        this.repository = repository;
    }

    private HostPowerOperationRequestMessage getHostPowerOperationRequestMessage(final ComponentEndpointIds componentEndpointIds,
            final String hostname)
    {
        final HostPowerOperationRequestMessage requestMessage = new HostPowerOperationRequestMessage();
        final PowerOperationRequest powerOperationRequest = new PowerOperationRequest();
        powerOperationRequest.setPowerOperation(PowerOperationRequest.PowerOperation.REBOOT);
        powerOperationRequest.setHostName(hostname);
        requestMessage.setPowerOperationRequest(powerOperationRequest);
        requestMessage.setCredentials(new Credentials(componentEndpointIds.getEndpointUrl(), null, null));
        requestMessage.setComponentEndpointIds(
                new com.dell.cpsd.virtualization.capabilities.api.ComponentEndpointIds(componentEndpointIds.getComponentUuid(),
                        componentEndpointIds.getEndpointUuid(), componentEndpointIds.getCredentialUuid()));
        return requestMessage;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        LOGGER.info("Execute Reboot Host task");

        final String taskMessage = "Reboot Host";
        final String hostname = (String) delegateExecution.getVariable(HOSTNAME);
        final NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);

        ComponentEndpointIds componentEndpointIds = null;
        try
        {
            componentEndpointIds = repository.getVCenterComponentEndpointIdsByEndpointType("VCENTER-CUSTOMER");
        }
        catch (Exception e)
        {
            LOGGER.error("An Unexpected Exception occurred attempting to retrieve VCenter Component Endpoints.", e);
            updateDelegateStatus(
                    "An Unexpected Exception occurred attempting to retrieve VCenter Component Endpoints.  Reason: " + e.getMessage());
            throw new BpmnError(REBOOT_HOST_FAILED,
                    "An Unexpected Exception occurred attempting to retrieve VCenter Component Endpoints.  Reason: " + e.getMessage());
        }

        final HostPowerOperationRequestMessage requestMessage = getHostPowerOperationRequestMessage(componentEndpointIds, hostname);

        boolean succeeded = false;

        try
        {
            this.nodeService.requestHostReboot(requestMessage);
        }
        catch (Exception e)
        {
            LOGGER.error("An Unexpected Exception occurred attempting to request " + taskMessage, e);
            updateDelegateStatus("An Unexpected Exception occurred attempting to request " + taskMessage + ".  Reason: " + e.getMessage());
            throw new BpmnError(REBOOT_HOST_FAILED,
                    "An Unexpected Exception occurred attempting to request " + taskMessage + ".  Reason: " + e.getMessage());
        }
        if (!succeeded)
        {
            LOGGER.error(taskMessage + " on Node " + nodeDetail.getServiceTag() + " failed!");
            updateDelegateStatus(taskMessage + " on Node " + nodeDetail.getServiceTag() + " failed!");
            throw new BpmnError(REBOOT_HOST_FAILED, taskMessage + " on Node " + nodeDetail.getServiceTag() + " failed!");
        }

        LOGGER.info(taskMessage + " on Node " + nodeDetail.getServiceTag() + " was successful.");
        updateDelegateStatus(taskMessage + " on Node " + nodeDetail.getServiceTag() + " was successful.");
    }
}
