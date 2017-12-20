/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.COMPLETED_NODE_DETAILS;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.MARK_NODE_DETAIL_COMPLETED_FAILED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;

@Component
@Scope("prototype")
@Qualifier("markNodeDetailCompleted")
public class MarkNodeDetailCompleted extends BaseWorkflowDelegate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MarkNodeDetailCompleted.class);

    private final RuntimeService runtimeService;

    @Autowired
    public MarkNodeDetailCompleted(RuntimeService runtimeService)
    {
        super(LOGGER, "Mark Node Detail Completed");
        this.runtimeService = runtimeService;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);
        updateDelegateStatus("Attempting " + this.taskName + " on Node " + nodeDetail.getServiceTag() + ".");

        nodeDetail.setCompleted(true);

        try
        {
            List<NodeDetail> nodeDetailList = (List<NodeDetail>) runtimeService
                    .getVariable(delegateExecution.getSuperExecution().getProcessInstanceId(), COMPLETED_NODE_DETAILS);

            nodeDetailList.add(nodeDetail);
        }
        catch (Exception ex)
        {
            final String message =
                    "An Unexpected Exception occurred attempting to mark the node status as completed on Node " + nodeDetail.getServiceTag()
                            + ". Reason: ";
            updateDelegateStatus(message);
            throw new BpmnError(MARK_NODE_DETAIL_COMPLETED_FAILED, message);
        }

        updateDelegateStatus(taskName + " on Node " + nodeDetail.getServiceTag() + " was successful.");
    }
}
