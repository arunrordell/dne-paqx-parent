/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.sdk.AMQPClient;
import com.dell.cpsd.service.system.definition.api.ComponentsFilter;
import com.dell.cpsd.service.system.definition.api.ConvergedSystem;
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.ADD_NODES_TO_SYSTEM_DEFINITION_MESSAGE_ID;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.COMPLETED_NODE_DETAILS;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.COMPLETE_ADD_NODES_TO_SYSTEM_DEFINITION_FAILED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.SUCCEEDED;

@Component
@Scope("prototype")
@Qualifier("completeAddNodesToSystemDefinition")
public class CompleteAddNodesToSystemDefinition extends BaseWorkflowDelegate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteAddNodesToSystemDefinition.class);

    private final AMQPClient sdkAMQPClient;

    @Autowired
    public CompleteAddNodesToSystemDefinition(final AMQPClient sdkAMQPClient)
    {
        super(LOGGER, "Complete Add Nodes To System Definition");
        this.sdkAMQPClient = sdkAMQPClient;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        final List<NodeDetail> completedNodeDetails = (List<NodeDetail>) delegateExecution.getVariable(COMPLETED_NODE_DETAILS);
        updateDelegateStatus("Attempting " + this.taskName + " on the following completed Nodes " + StringUtils
                .join(completedNodeDetails.stream().map(NodeDetail::getServiceTag).collect(Collectors.toList()) + "."));

        try
        {
            String runnableResult = (String) delegateExecution.getVariable(ADD_NODES_TO_SYSTEM_DEFINITION_MESSAGE_ID);
            if (StringUtils.isEmpty(runnableResult) || !runnableResult.equals(SUCCEEDED))
            {
                throw new IllegalStateException(runnableResult);
            }

            final List<ConvergedSystem> allConvergedSystems = this.sdkAMQPClient.getConvergedSystems();
            if (CollectionUtils.isEmpty(allConvergedSystems))
            {
                throw new IllegalStateException("No converged systems found.");
            }

            final ConvergedSystem system = allConvergedSystems.get(0);
            final ComponentsFilter componentsFilter = new ComponentsFilter();
            componentsFilter.setSystemUuid(system.getUuid());

            final List<ConvergedSystem> updateSystemDetails = this.sdkAMQPClient.getComponents(componentsFilter);
            final ConvergedSystem systemThatWasUpdated = updateSystemDetails.get(0);

            final List<NodeDetail> additions = completedNodeDetails.stream().filter(Objects::nonNull).filter(nodeDetail ->
                    systemThatWasUpdated.getComponents().stream().filter(Objects::nonNull)
                            .filter(component -> component.getIdentity().getIdentifier().equals(nodeDetail.getId()))
                            .collect(Collectors.toList()).size() > 0).collect(Collectors.toList());

            if (additions.size() != completedNodeDetails.size())
            {
                final Set<String> serviceTags = additions.stream().map(NodeDetail::getServiceTag).collect(Collectors.toSet());

                final List<NodeDetail> failedAdditions = completedNodeDetails.stream()
                        .filter(nodeDetail -> !serviceTags.contains(nodeDetail.getServiceTag())).collect(Collectors.toList());

                throw new IllegalStateException("The following discovered nodes were not added to the system definition: " + StringUtils
                        .join(failedAdditions.stream().map(NodeDetail::getServiceTag).collect(Collectors.toList()), ", "));
            }
        }
        catch (Exception e)
        {
            final String message = taskName + " failed!  Reason: ";
            updateDelegateStatus(message, e);
            throw new BpmnError(COMPLETE_ADD_NODES_TO_SYSTEM_DEFINITION_FAILED, message + e.getMessage());
        }

        updateDelegateStatus(taskName + " was successful.");
    }
}
