/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.task.handler.preprocess;

import com.dell.cpsd.paqx.dne.domain.IWorkflowTaskHandler;
import com.dell.cpsd.paqx.dne.domain.Job;
import com.dell.cpsd.paqx.dne.domain.node.NodeInventory;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.model.Status;
import com.dell.cpsd.paqx.dne.service.model.TaskResponse;
import com.dell.cpsd.paqx.dne.service.task.handler.BaseTaskHandler;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import com.dell.cpsd.service.common.client.exception.ServiceTimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This class implements the logic to discover RackHD Node Inventory.
 *
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */
public class DiscoverNodeInventoryTaskHandler extends BaseTaskHandler implements IWorkflowTaskHandler
{
    /*
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoverNodeInventoryTaskHandler.class);

    /*
     * Node Service instance used to find and validate Storage pool entries
     */
    private NodeService nodeService;

    /*
     * Reference to H2 repository
     */
    private final DataServiceRepository repository;

    /**
     * Construct an instance based on nodeservice reference
     *
     * @param nodeService
     */
    public DiscoverNodeInventoryTaskHandler(final NodeService nodeService, final DataServiceRepository repository)
    {
        this.nodeService = nodeService;
        this.repository = repository;
    }

    @Override
    public boolean executeTask(final Job job)  {
        LOGGER.info("Execute DiscoverNodeInventoryTaskHandler task.");

        TaskResponse response = initializeResponse(job);

        try {
            String symphonyUUID = job.getInputParams().getSymphonyUuid();

            Map<String, Object> nodeInventoryResponse = nodeService.listNodeInventory(symphonyUUID);
            if (nodeInventoryResponse != null) {
                NodeInventory nodeInventory = new NodeInventory(symphonyUUID, nodeInventoryResponse.get(symphonyUUID));
                boolean isNodeInventorySaved = repository.saveNodeInventory(nodeInventory);

                if (isNodeInventorySaved) {
                    response.setWorkFlowTaskStatus(Status.SUCCEEDED);

                    return true;
                }
            } else {
                LOGGER.info("There is no node inventory for UUID ", symphonyUUID);
                response.addError("There is no node inventory for UUID " + symphonyUUID);
            }
        } catch (ServiceTimeoutException | ServiceExecutionException | JsonProcessingException ex) {
            LOGGER.error("Node Inventory discover failed : ", ex);
            response.addError("Unable to discover node inventory.");
        }


        response.setWorkFlowTaskStatus(Status.FAILED);
        return false;
    }

}
