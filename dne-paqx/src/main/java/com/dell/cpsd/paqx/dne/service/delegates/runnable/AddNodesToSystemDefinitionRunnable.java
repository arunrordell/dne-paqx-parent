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
import com.dell.cpsd.service.cs.credential.service.api.Credential;
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
import java.util.Objects;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.SUCCEEDED;

public class AddNodesToSystemDefinitionRunnable implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AddNodesToSystemDefinitionRunnable.class);

    private static final String COMPONENT_SERVER_TEMPLATE = "COMMON-DELL-POWEREDGE";
    private final String                processId;
    private final String                activityId;
    private final String                messageId;
    private final List<NodeDetail>      nodeDetails;
    private final RuntimeService        runtimeService;
    private final AMQPClient            sdkAMQPClient;
    private final DataServiceRepository repository;

    /**
     * AddNodesToSystemDefinitionRunnable constructor
     *
     * @param processId
     * @param activityId
     * @param messageId
     * @param nodeDetails
     * @param runtimeService
     * @param sdkAMQPClient
     * @param repository
     */
    public AddNodesToSystemDefinitionRunnable(final String processId, final String activityId, final String messageId,
            final List<NodeDetail> nodeDetails, final RuntimeService runtimeService, final AMQPClient sdkAMQPClient,
            final DataServiceRepository repository)
    {
        this.processId = processId;
        this.activityId = activityId;
        this.messageId = messageId;
        this.nodeDetails = nodeDetails;
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

            List<Credential> credentialAdditions = new ArrayList<>();

            nodeDetails.stream().filter(Objects::nonNull).forEach(nodeDetail -> {
                final DiscoveredNodeInfo nodeInfo = repository.getDiscoveredNodeInfo(nodeDetail.getId());

                if (nodeInfo == null)
                {
                    throw new IllegalStateException("No discovered node info.");
                }

                final Component newComponent = new Component();
                final List<String> parentGroups = new ArrayList<>();
                final List<String> endpoints = new ArrayList<>();
                parentGroups.add("SystemCompute");
                endpoints.add("RACKHD-EP");
                newComponent.setUuid(nodeInfo.getSymphonyUuid());
                newComponent.setIdentity(
                        new Identity("SERVER", nodeInfo.getSymphonyUuid(), nodeInfo.getSymphonyUuid(), nodeInfo.getSerialNumber(), null));
                newComponent.setDefinition(
                        new Definition(nodeInfo.getProductFamily(), nodeInfo.getProduct(), nodeInfo.getModelFamily(), nodeInfo.getModel()));
                newComponent.setParentGroupUuids(this.mapGroupNamesToUUIDs(parentGroups, systemToBeUpdated.getGroups()));
                newComponent.setEndpoints(new ArrayList<>());
                List<Credential> newCredentials = this.sdkAMQPClient
                        .addComponentToConvergedSystem(systemToBeUpdated, newComponent, endpoints, COMPONENT_SERVER_TEMPLATE);
                credentialAdditions.addAll(newCredentials);
            });

            this.sdkAMQPClient.createOrUpdateConvergedSystem(systemToBeUpdated, credentialAdditions);
            result = SUCCEEDED;
        }
        catch (Exception ex)
        {
            final String message = "Add nodes to system definition failed! Reason: " + ex.getMessage();
            LOGGER.error(message);
            result = message;
        }

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId(activityId).singleResult();
        if (execution != null)
        {
            runtimeService.setVariable(execution.getId(), messageId, result);
            runtimeService.messageEventReceived(messageId, execution.getId());
        }
    }
}
