/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.DelegateRequestModel;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.transformers.AddHostToVCenterClusterRequestTransformer;
import com.dell.cpsd.virtualization.capabilities.api.ClusterOperationRequestMessage;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.ADD_HOST_TO_CLUSTER_FAILED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;

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
    private final NodeService                               nodeService;
    private final AddHostToVCenterClusterRequestTransformer requestTransformer;

    @Autowired
    public AddHostToVCenter(final NodeService nodeService, final AddHostToVCenterClusterRequestTransformer requestTransformer)
    {
        super(LOGGER, "Add ESXi Host to VCenter Cluster");
        this.nodeService = nodeService;
        this.requestTransformer = requestTransformer;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);
        updateDelegateStatus("Attempting " + this.taskName + " on Node " + nodeDetail.getServiceTag() + ".");
        try
        {
            final DelegateRequestModel<ClusterOperationRequestMessage> delegateRequestModel = requestTransformer
                    .buildAddHostToVCenterRequest(delegateExecution);
            this.nodeService.requestAddHostToVCenter(delegateRequestModel.getRequestMessage());

            final String returnMessage = this.taskName + " on Node " + nodeDetail.getServiceTag() + " was successful.";
            updateDelegateStatus(returnMessage);
        }
        catch (Exception ex)
        {
            String errorMessage = "An unexpected exception occurred attempting to " + this.taskName + ". Reason: ";
            updateDelegateStatus(errorMessage + ex.getMessage(), ex);
            throw new BpmnError(ADD_HOST_TO_CLUSTER_FAILED, errorMessage + ex.getMessage());
        }
    }
}
