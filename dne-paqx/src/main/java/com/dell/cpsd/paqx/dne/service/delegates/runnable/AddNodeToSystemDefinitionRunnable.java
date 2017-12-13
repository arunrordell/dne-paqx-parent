/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates.runnable;

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
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.SUCCEEDED;

public class AddNodeToSystemDefinitionRunnable implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AddNodeToSystemDefinitionRunnable.class);

    private static final String COMPONENT_SERVER_TEMPLATE = "COMMON-DELL-POWEREDGE";
    private final String                processId;
    private final String                activityId;
    private final String                messageId;
    private final NodeDetail            nodeDetail;
    private final RuntimeService        runtimeService;
    private final AMQPClient            sdkAMQPClient;
    private final DataServiceRepository repository;

    public AddNodeToSystemDefinitionRunnable(final String processId, final String activityId, final String messageId,
            final NodeDetail nodeDetail, final RuntimeService runtimeService, final AMQPClient sdkAMQPClient,
            final DataServiceRepository repository)
    {
        this.processId = processId;
        this.activityId = activityId;
        this.messageId = messageId;
        this.nodeDetail = nodeDetail;
        this.runtimeService = runtimeService;
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
    public void run()
    {
        String result;

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
            result = SUCCEEDED;
        }
        catch (Exception ex)
        {
            final String message =
                    "Add node to system definition on Node " + nodeDetail.getServiceTag() + " failed! Reason: " + ex.getMessage();
            LOGGER.error(message);
            result = message;
        }

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId(activityId).singleResult();
        runtimeService.setVariable(execution.getId(), messageId, result);
        runtimeService.messageEventReceived(messageId, execution.getId());
    }
}
