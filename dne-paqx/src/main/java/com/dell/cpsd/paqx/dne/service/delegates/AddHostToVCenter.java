/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.ESXiCredentialDetails;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds;
import com.dell.cpsd.virtualization.capabilities.api.ClusterOperationRequest;
import com.dell.cpsd.virtualization.capabilities.api.ClusterOperationRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.Credentials;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.ADD_HOST_TO_CLUSTER_FAILED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.ESXI_CREDENTIAL_DETAILS;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.HOSTNAME;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.VCENTER_CLUSTER_ID;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.VCENTER_CLUSTER_NAME;

@Component
@Scope("prototype")
@Qualifier("addHostToVCenter")
public class AddHostToVCenter extends BaseWorkflowDelegate
{
    /**
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AddHostToVCenter.class);

    /**
     * The <code>NodeService</code> instance
     */
    private final NodeService nodeService;
    private final DataServiceRepository repository;

    @Autowired
    public AddHostToVCenter(final NodeService nodeService, final DataServiceRepository repository)
    {
        this.nodeService = nodeService;
        this.repository = repository;
    }

    private ClusterOperationRequestMessage getClusterOperationRequestMessage(
            final ComponentEndpointIds componentEndpointIds, final String hostname, final String clusterId,
            final ESXiCredentialDetails esxiCredentialDetails)
    {
        final ClusterOperationRequestMessage requestMessage = new ClusterOperationRequestMessage();

        requestMessage.setCredentials(new Credentials(componentEndpointIds.getEndpointUrl(), null, null));
        requestMessage.setComponentEndpointIds(new com.dell.cpsd.virtualization.capabilities.api.ComponentEndpointIds(
                componentEndpointIds.getComponentUuid(), componentEndpointIds.getEndpointUuid(),
                componentEndpointIds.getCredentialUuid()));
        final ClusterOperationRequest clusterOperationRequest = new ClusterOperationRequest();
        clusterOperationRequest.setHostName(hostname);
        clusterOperationRequest.setComponentEndpointIds(
                new com.dell.cpsd.virtualization.capabilities.api.ComponentEndpointIds(
                        esxiCredentialDetails.getComponentUuid(), esxiCredentialDetails.getEndpointUuid(),
                        esxiCredentialDetails.getCredentialUuid()));
        clusterOperationRequest.setClusterOperation(ClusterOperationRequest.ClusterOperation.ADD_HOST);
        clusterOperationRequest.setClusterID(clusterId);
        requestMessage.setClusterOperationRequest(clusterOperationRequest);
        return requestMessage;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        LOGGER.info("Execute Add Host to VCenter");
        final NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);
        final String clusterName = (String) delegateExecution.getVariable(VCENTER_CLUSTER_NAME);
        final ComponentEndpointIds componentEndpointIds = repository.getVCenterComponentEndpointIdsByEndpointType(
                "VCENTER-CUSTOMER");
        final String hostname = (String) delegateExecution.getVariable(HOSTNAME);
        final ESXiCredentialDetails esxiCredentialDetails = (ESXiCredentialDetails) delegateExecution.getVariable(
                ESXI_CREDENTIAL_DETAILS);
        final String clusterId = repository.getClusterId(clusterName);

        final ClusterOperationRequestMessage requestMessage = getClusterOperationRequestMessage(componentEndpointIds,
                                                                                                hostname, clusterId,
                                                                                                esxiCredentialDetails);
        final boolean success = this.nodeService.requestAddHostToVCenter(requestMessage);
        if (!success)
        {
            LOGGER.error("Add Host to VCenter on Node " + nodeDetail.getServiceTag() + " failed!");
            updateDelegateStatus("Add Host to VCenter on Node " + nodeDetail.getServiceTag() + " failed!");
            throw new BpmnError(ADD_HOST_TO_CLUSTER_FAILED,
                                "Add Host to VCenter on Node " + nodeDetail.getServiceTag() + " failed!");
        }
        delegateExecution.setVariable(VCENTER_CLUSTER_ID, clusterId);
        LOGGER.info("Add Host to VCenter on Node " + nodeDetail.getServiceTag() + " was successful.");
        updateDelegateStatus("Add Host to VCenter on Node " + nodeDetail.getServiceTag() + " was successful.");


    }
}
