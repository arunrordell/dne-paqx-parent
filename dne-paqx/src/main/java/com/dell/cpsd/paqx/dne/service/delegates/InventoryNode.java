/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.domain.node.NodeInventory;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import com.dell.cpsd.service.common.client.exception.ServiceTimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections.MapUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAILS;

@Component
@Scope("prototype")
@Qualifier("inventoryNode")
public class InventoryNode extends BaseWorkflowDelegate
{
    /*
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryNode.class);

    /*
     * Node Service instance used to find and validate Storage pool entries
     */
    private NodeService nodeService;

    /*
     * Reference to H2 repository
     */
    private final DataServiceRepository repository;

    @Autowired
    public InventoryNode(final NodeService nodeService, final DataServiceRepository repository)
    {
        super(LOGGER, "Update Node Inventory");
        this.nodeService = nodeService;
        this.repository = repository;
    }


    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {

        List<NodeDetail> nodeDetails = (List<NodeDetail> ) delegateExecution.getVariable(NODE_DETAILS);

        Map<String, Object> nodeInventoryResponse = null;
        try
        {
            updateDelegateStatus("Requesting Update Node Inventory.");
            nodeInventoryResponse = nodeService.listNodeInventory(null);
        }
        catch (ServiceTimeoutException | ServiceExecutionException ex)
        {
            final String message = "Update Node Inventory request failed.";
            updateDelegateStatus(message, ex);
            nodeDetails.stream().filter(Objects::nonNull).forEach(nodeDetail -> nodeDetail.setInventoryFailed(true));
        }

        if (MapUtils.isNotEmpty(nodeInventoryResponse))
        {
            List<String> nodeIds = new ArrayList<>();
            nodeInventoryResponse.entrySet().stream().filter(Objects::nonNull).forEach(entry -> {
                NodeInventory nodeInventory = null;
                try
                {
                    nodeInventory = new NodeInventory(entry.getKey(), entry.getValue());
                    nodeIds.add(entry.getKey());
                }
                catch (JsonProcessingException jpe)
                {
                    final String message = "Update Node Inventory failed due to unrecognized response for Node with uuid " + entry.getKey();
                    updateDelegateStatus(message, jpe);
                    nodeDetails.stream().filter(Objects::nonNull).forEach(nodeDetail -> nodeDetail.setInventoryFailed(true));
                }

                boolean isNodeInventorySaved = false;
                if (nodeInventory != null)
                {
                    isNodeInventorySaved = repository.saveNodeInventory(nodeInventory);
                }
                if (!isNodeInventorySaved)
                {
                    final String message = "Update Node Inventory on Node with uuid " + entry.getKey() + " failed.";
                    updateDelegateStatus(message);
                    nodeDetails.stream().filter(Objects::nonNull).forEach(nodeDetail -> nodeDetail.setInventoryFailed(true));
                }
            });

            updateDelegateStatus("Update Node Inventory was successful for Nodes " + Arrays.toString(nodeIds.toArray()));
        }
        else
        {
            final String message = "Could not find any node inventory.";
            updateDelegateStatus(message);
            nodeDetails.stream().filter(Objects::nonNull).forEach(nodeDetail -> nodeDetail.setInventoryFailed(true));
        }

    }
}
