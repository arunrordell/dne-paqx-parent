/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.COMPLETED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.COMPLETED_NODE_DETAILS;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NOTIFY_NODE_STATUS_COMPLETED_FAILED;

@Component
@Scope("prototype")
@Qualifier("notifyNodeDiscoveryToUpdateStatusCompleted")
public class NotifyNodeDiscoveryToUpdateStatusCompleted extends BaseWorkflowDelegate
{
    /*
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyNodeDiscoveryToUpdateStatusCompleted.class);

    /*
     * The <code>NodeService</code> instance
     */
    private final NodeService nodeService;

    /**
     * NotifyNodeDiscoveryToUpdateStatusCompleted constructor.
     *
     * @param nodeService - The <code>NodeService</code> instance.
     * @since 1.0
     */
    @Autowired
    public NotifyNodeDiscoveryToUpdateStatusCompleted(NodeService nodeService)
    {
        super(LOGGER, "Update Node Status");
        this.nodeService = nodeService;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        final List<NodeDetail> completedNodeDetails = (List<NodeDetail>) delegateExecution.getVariable(COMPLETED_NODE_DETAILS);

        updateDelegateStatus("Attempting " + this.taskName + " on the following Nodes " + StringUtils
                .join(completedNodeDetails.stream().map(NodeDetail::getServiceTag).collect(Collectors.toList()) + "."));

        List<NodeDetail> failedUpdates = new ArrayList<>();

        completedNodeDetails.stream().filter(Objects::nonNull).forEach(nd -> {
            try
            {
                final boolean succeeded = this.nodeService.notifyNodeAllocationStatus(nd.getId(), COMPLETED);

                if (!succeeded)
                {
                    final String message = "Updating node status to completed failed on Node " + nd.getServiceTag() + "failed.";
                    LOGGER.error(message);
                    failedUpdates.add(nd);
                }
            }
            catch (Exception e)
            {
                final String message =
                        "An Unexpected Exception occurred attempting to update node status to completed on Node " + nd.getServiceTag()
                                + ". Reason: ";
                LOGGER.error(message);
                failedUpdates.add(nd);
            }
        });

        if (CollectionUtils.isNotEmpty(failedUpdates))
        {
            final String message = "Updating Node Status to completed failed for the following Nodes: " + StringUtils
                    .join(failedUpdates.stream().map(NodeDetail::getServiceTag).collect(Collectors.toList()) + ".");
            updateDelegateStatus(message);
            throw new BpmnError(NOTIFY_NODE_STATUS_COMPLETED_FAILED, message);
        }

        updateDelegateStatus(taskName + " was successful.");
    }
}
