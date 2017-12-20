/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.sdk.AMQPClient;
import com.dell.cpsd.service.system.definition.api.Component;
import com.dell.cpsd.service.system.definition.api.ComponentsFilter;
import com.dell.cpsd.service.system.definition.api.ConvergedSystem;
import com.dell.cpsd.service.system.definition.api.Definition;
import com.dell.cpsd.service.system.definition.api.Group;
import com.dell.cpsd.service.system.definition.api.Identity;
import org.apache.commons.collections.CollectionUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.ADD_NODES_TO_SYSTEM_DEFINITION_FAILED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;

@org.springframework.stereotype.Component
@Scope("prototype")
@Qualifier("addNodeToSystemDefinition")
public class AddNodeToSystemDefinition extends BaseWorkflowDelegate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AddNodeToSystemDefinition.class);

    private static final String COMPONENT_SERVER_TEMPLATE = "COMMON-DELL-POWEREDGE";

    private final AMQPClient            sdkAMQPClient;
    private final DataServiceRepository repository;

    /**
     * AddNodeToSystemDefinition constructor.
     *
     * @param sdkAMQPClient - The <code>AMQPClient</code> instance.
     * @param repository    - The <code>DataServiceRepository</code> instance.
     * @since 1.0
     */
    @Autowired
    public AddNodeToSystemDefinition(AMQPClient sdkAMQPClient, DataServiceRepository repository)
    {
        super(LOGGER, "Add Node To System Definition");
        this.sdkAMQPClient = sdkAMQPClient;
        this.repository = repository;
    }

    /*
     * Given a list of group names map each one to its corresponding UUID.
     */
    private List<String> mapGroupNamesToUUIDs(List<String> groupNames, List<Group> groups)
    {
        List<String> groupUuids = new ArrayList<>();
        groups.forEach(group -> {
            if (groupNames.contains(group.getName()))
            {
                groupUuids.add(group.getUuid());
            }
        });
        return groupUuids;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        final NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);
        updateDelegateStatus("Attempting " + this.taskName + " on Node " + nodeDetail.getServiceTag() + ".");
        try
        {
            final List<ConvergedSystem> allConvergedSystems = this.sdkAMQPClient.getConvergedSystems();
            if (CollectionUtils.isEmpty(allConvergedSystems))
            {
                throw new IllegalStateException("No converged systems found.");
            }

            final ConvergedSystem system = allConvergedSystems.get(0);
            final ComponentsFilter componentsFilter = new ComponentsFilter();
            componentsFilter.setSystemUuid(system.getUuid());

            final List<ConvergedSystem> systemDetails = this.sdkAMQPClient.getComponents(componentsFilter);
            if (CollectionUtils.isEmpty(systemDetails))
            {
                throw new IllegalStateException("No converged system found.");
            }

            final ConvergedSystem systemToBeUpdated = systemDetails.get(0);

            final DiscoveredNodeInfo nodeInfo = repository.getDiscoveredNodeInfo(nodeDetail.getId());

            if (nodeInfo == null)
            {
                throw new IllegalStateException("No discovered node info.");
            }

            final Component newNode = new Component();
            final List<String> parentGroups = new ArrayList<>();
            final List<String> endpoints = new ArrayList<>();
            parentGroups.add("SystemCompute");
            endpoints.add("RACKHD-EP");
            newNode.setUuid(nodeInfo.getSymphonyUuid());
            newNode.setIdentity(
                    new Identity("SERVER", nodeInfo.getSymphonyUuid(), nodeInfo.getSymphonyUuid(), nodeInfo.getSerialNumber(), null));
            newNode.setDefinition(
                    new Definition(nodeInfo.getProductFamily(), nodeInfo.getProduct(), nodeInfo.getModelFamily(), nodeInfo.getModel()));
            newNode.setParentGroupUuids(this.mapGroupNamesToUUIDs(parentGroups, systemToBeUpdated.getGroups()));
            newNode.setEndpoints(new ArrayList<>());

            this.sdkAMQPClient.addComponent(systemToBeUpdated, newNode, endpoints, COMPONENT_SERVER_TEMPLATE);

            // Need to make sure the node was really added.
            // The SDK has a habit of swallowing exceptions making it difficult to know
            // if there was an error...
            final List<ConvergedSystem> updateSystemDetails = this.sdkAMQPClient.getComponents(componentsFilter);
            final ConvergedSystem systemThatWasUpdated = updateSystemDetails.get(0);

            final Component newComponent = systemThatWasUpdated.getComponents().stream()
                    .filter(c -> c.getIdentity().getIdentifier().equals(newNode.getIdentity().getIdentifier())).findFirst().orElse(null);

            if (newComponent == null)
            {
                throw new IllegalStateException("The discovered node was not added to the system definition.");
            }
        }
        catch (Exception ex)
        {
            final String message = this.taskName + " on Node " + nodeDetail.getServiceTag() + " failed! Reason: ";
            updateDelegateStatus(message, ex );
            throw new BpmnError(ADD_NODES_TO_SYSTEM_DEFINITION_FAILED, message + ex.getMessage());
        }

        final String message = this.taskName + " on Node " + nodeDetail.getServiceTag() + " was successful.";
        updateDelegateStatus(message);
    }

}
