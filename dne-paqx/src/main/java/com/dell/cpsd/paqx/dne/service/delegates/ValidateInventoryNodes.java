/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import org.apache.commons.collections.CollectionUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.FAILED_NODE_DETAILS;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.INVENTORY_NODES_FAILED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAILS;

@Component
@Scope("prototype")
@Qualifier("validateInventoryNodes")
public class ValidateInventoryNodes extends BaseWorkflowDelegate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyNodesSelected.class);

    @Autowired
    public ValidateInventoryNodes()
    {
        super(LOGGER, "Validate Node Inventories Updated");
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        List<NodeDetail> nodeDetails = (List<NodeDetail>) delegateExecution.getVariable(NODE_DETAILS);
        if (CollectionUtils.isNotEmpty(nodeDetails)) {
            List<NodeDetail> failedNodes = nodeDetails.stream().filter(nodeDetail -> nodeDetail.isInventoryFailed()).collect(
                    Collectors.toList());
            if (CollectionUtils.isNotEmpty(failedNodes)) {
                nodeDetails.removeAll(failedNodes);
                delegateExecution.setVariable(NODE_DETAILS, nodeDetails);
                delegateExecution.setVariable(FAILED_NODE_DETAILS, failedNodes);
                final String message = "Update of Node Inventories was not successful for Nodes " + failedNodes.stream().map(NodeDetail::getServiceTag).collect(Collectors.joining(", ")) + ".";
                updateDelegateStatus(message);
                throw new BpmnError(INVENTORY_NODES_FAILED, message);
            }
        }
        updateDelegateStatus("All Node Inventories were updated successfully.");
    }
}
