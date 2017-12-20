/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.rest.controller;

import com.dell.cpsd.paqx.dne.rest.exception.WorkflowNotFoundException;
import com.dell.cpsd.paqx.dne.rest.model.AboutInfo;
import com.dell.cpsd.paqx.dne.rest.model.Node;
import com.dell.cpsd.paqx.dne.service.ICamundaWorkflowService;
import com.dell.cpsd.paqx.dne.service.delegates.exception.JobNotFoundException;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import com.dell.cpsd.paqx.dne.service.model.multinode.Job;
import com.dell.cpsd.paqx.dne.service.model.multinode.Status;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@EnableAsync
@RestController
@RequestMapping("/multinode")
public class MultiNodeController
{
    /*
     * The logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiNodeController.class);

    private ICamundaWorkflowService camundaWorkflowService;

    /**
     * The service controlling the workflow.
     */
    // commented because has dependencies on missed classes
    @Autowired
    public MultiNodeController(final ICamundaWorkflowService camundaWorkflowService)
    {
        this.camundaWorkflowService = camundaWorkflowService;
    }

    /**
     * Send request to get about info for node expansion PAQX.
     *
     * @return Returns about information for node expansion PAQX.
     */
    @CrossOrigin
    @RequestMapping(path = "/about", method = RequestMethod.GET, produces = "application/json")
    public AboutInfo about()
    {
        return new AboutInfo("Multi Node Expansion API v0.1");
    }

    @CrossOrigin
    @RequestMapping(path = "/preprocess", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public Job startPreProcessWorkflow(@RequestBody List<Node> nodesList) throws InterruptedException, ExecutionException
    {
        Job responseJob = null;

        Map<String, Object> inputVariables = new HashMap<>();
        if (CollectionUtils.isNotEmpty(nodesList))
        {
            List<NodeDetail> nodeDetailsList = nodesList.stream().map(node -> new NodeDetail(node.getId(), node.getServiceTag()))
                    .collect(Collectors.toList());
            inputVariables.put(DelegateConstants.NODE_DETAILS, nodeDetailsList);
        }

        String jobId = camundaWorkflowService.startWorkflow("preProcess", inputVariables);
        if (jobId != null)
        {
            responseJob = new Job();
            responseJob.setId(jobId);
        }
        return responseJob;
    }

    @CrossOrigin
    @RequestMapping(path = "/addnodes", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public Job startAddNodesWorkflow(@RequestBody List<NodeDetail> nodeList) throws InterruptedException, ExecutionException
    {
        Job responseJob = null;

        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put(DelegateConstants.NODE_DETAILS, nodeList);
        inputVariables.put(DelegateConstants.COMPLETED_NODE_DETAILS, Collections.synchronizedList(new ArrayList<NodeDetail>()));

        String jobId = camundaWorkflowService.startWorkflow("addNodes", inputVariables);
        if (jobId != null)
        {
            responseJob = new Job();
            responseJob.setId(jobId);
        }
        return responseJob;
    }

    @CrossOrigin
    @RequestMapping(path = "/status/{jobId}", method = RequestMethod.GET, produces = "application/json")
    public Status getStatus(final @PathVariable String jobId) throws WorkflowNotFoundException
    {
        Status status = null;
        try
        {
            status = camundaWorkflowService.getStatus(jobId);
            if (status == null)
            {
                String errMsg = "Can not find the workflow with jobId " + jobId;
                LOGGER.error(errMsg);
                throw new WorkflowNotFoundException(HttpStatus.NOT_FOUND.value(), errMsg);
            }
        }
        catch (JobNotFoundException jnfe)
        {
            throw new WorkflowNotFoundException(HttpStatus.NOT_FOUND.value(), jnfe.getMessage(), jnfe);
        }
        return status;
    }
}