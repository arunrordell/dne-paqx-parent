/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */
package com.dell.cpsd.paqx.dne.rest.controller;

import com.dell.cpsd.paqx.dne.domain.Job;
import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.paqx.dne.rest.exception.WorkflowNotFoundException;
import com.dell.cpsd.paqx.dne.rest.model.AboutInfo;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.WorkflowService;
import com.dell.cpsd.paqx.dne.service.model.IdracInfo;
import com.dell.cpsd.paqx.dne.service.model.IdracNetworkSettingsRequest;
import com.dell.cpsd.paqx.dne.service.model.NodeExpansionRequest;
import com.dell.cpsd.paqx.dne.service.model.NodeExpansionResponse;
import com.dell.cpsd.paqx.dne.service.model.NodeInfo;
import com.dell.cpsd.paqx.dne.service.model.NodeStatus;
import com.dell.cpsd.paqx.dne.service.orchestration.IOrchestrationService;
import com.dell.cpsd.paqx.dne.service.workflow.addnode.IAddNodeService;
import com.dell.cpsd.paqx.dne.service.workflow.preprocess.IPreProcessService;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import com.dell.cpsd.service.common.client.exception.ServiceTimeoutException;
import com.dell.cpsd.virtualization.capabilities.api.ClusterInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */

@EnableAsync
@RestController
@RequestMapping("/dne")
public class NodeExpansionController
{
    /*
     * The logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeExpansionController.class);

    
    /*
     * The service controlling the workflow.
     */
    @Autowired
    private IPreProcessService preProcessService;

    @Autowired
    private IAddNodeService addNodeService;

    @Autowired
    private IOrchestrationService orchestrationService;

    public NodeService getNodeService() {
        return nodeService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Autowired
    private NodeService nodeService;
    /**
     * Send request to get about info for node expansion PAQX.
     * @return Returns about information for node expansion PAQX.
     */
    @CrossOrigin
    @RequestMapping(path="/about", method = RequestMethod.GET, produces = "application/json")
    public AboutInfo about(HttpServletResponse httpServletResponse) {
        return new AboutInfo("Node Expansion API v0.1");
    }

    /**
     * List discovered nodes
     *
     * @param servletRequest
     * @return
     * @throws ServiceTimeoutException
     * @throws ServiceExecutionException
     */
    @CrossOrigin
    @RequestMapping(path = "/nodes", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public List<NodeInfo> listDiscoveredNodes(HttpServletRequest servletRequest) throws ServiceTimeoutException, ServiceExecutionException, JsonProcessingException {
        List<DiscoveredNodeInfo> discoveredNodes = nodeService.listDiscoveredNodeInfo();
        if ( discoveredNodes.size()!= 0)
        {
            return discoveredNodes.stream().map( n -> new NodeInfo( n.getSymphonyUuid(), NodeStatus.valueOf(n.getNodeStatus().toString()),
                    n.getSerialNumber(),n.getProduct(),n.getVendor())).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /**
     * List discovered nodes
     *
     * @param servletRequest
     * @return
     * @throws ServiceTimeoutException
     * @throws ServiceExecutionException
     */
    @CrossOrigin
    @RequestMapping(path = "/firstnode", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public NodeInfo GetFirstDiscoveredNodes(HttpServletRequest servletRequest) throws ServiceTimeoutException, ServiceExecutionException, JsonProcessingException {
        DiscoveredNodeInfo discoveredNode = nodeService.getFirstDiscoveredNodeInfo();
        if ( discoveredNode != null )
        {
            return  new NodeInfo( discoveredNode.getSymphonyUuid(), NodeStatus.valueOf(discoveredNode.getNodeStatus().toString()),
                    discoveredNode.getSerialNumber(),discoveredNode.getProduct(),discoveredNode.getVendor());
        }
        return null;
    }

    @CrossOrigin
    @RequestMapping(path = "/preprocess", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<NodeExpansionResponse> preProcessOrchestration(@RequestBody NodeExpansionRequest params,
                                                                         HttpServletRequest servletRequest) throws InterruptedException, ExecutionException
    {
        LOGGER.info("Input request: " + params.toString());
        String uuid = params.getSymphonyUuid();

        if ( uuid == null || uuid.isEmpty()) {
            String errMsg = "Symphony uuid is required field for preprocess workflow.";
            LOGGER.error(errMsg);
            NodeExpansionResponse ner = new NodeExpansionResponse();
            ner.setErrors(Collections.singletonList(errMsg));
            return  new ResponseEntity<>(ner, HttpStatus.BAD_REQUEST);
        }

        Job job = preProcessService.createWorkflow("preProcessWorkflow", "startPreProcessWorkflow",
                "PreProcess request has been Submitted successfully");
        job.setInputParams(params);
        orchestrationService.orchestrateWorkflow(job, preProcessService );

        return new ResponseEntity<>(preProcessService.makeNodeExpansionResponse(job), HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(path = "/preprocess/step/{stepName}", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<NodeExpansionResponse> preProcessStep(@PathVariable String stepName,
                                                                        @RequestBody NodeExpansionRequest params,
                                                                        HttpServletRequest servletRequest) throws InterruptedException, ExecutionException, WorkflowNotFoundException {
        Job job = preProcessService.createWorkflow("preprocess", stepName,
                "Preprocess request has been Submitted successfully");
        LOGGER.info("Input request: " + params.toString());

        if ( job == null ) {
            String errMsg = "Can not find the workflow with step name " + stepName;
            LOGGER.error( errMsg);
            throw new WorkflowNotFoundException(HttpStatus.NOT_FOUND.value(), errMsg);
        }
        orchestrationService.orchestrateWorkflow(job, preProcessService);

        return new ResponseEntity<>(preProcessService.makeNodeExpansionResponse(job), HttpStatus.OK);
    }

    /**
     * This starts the add node process.
     * 
     * @param   params          The request parameters
     * @param   servletRequest  The servlet request.
     * 
     * @return   The node expansion response entity.
     * 
     * @since   1.0
     */
    @CrossOrigin
    @RequestMapping(path = "/nodes", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<NodeExpansionResponse> addNode(@RequestBody NodeExpansionRequest params, 
            HttpServletRequest servletRequest) throws InterruptedException, ExecutionException
    {
        Job job = addNodeService.createWorkflow("addNode", "startAddNodeWorkflow",
                "AddNode request has been Submitted successfully");
        LOGGER.info("Input request: " + params.toString());
        job.setInputParams(params);
        orchestrationService.orchestrateWorkflow(job, addNodeService );

        return new ResponseEntity<>(addNodeService.makeNodeExpansionResponse(job), HttpStatus.OK);
    }


    @CrossOrigin
    @RequestMapping(path = "/nodes/step/{stepName}", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<NodeExpansionResponse> dontKnowWhatToCallThis(@PathVariable String stepName,
                                                                        @RequestBody NodeExpansionRequest params,
                                                                        HttpServletRequest servletRequest) throws InterruptedException, ExecutionException, WorkflowNotFoundException {
        Job job = addNodeService.createWorkflow("addNode", stepName,
                "AddNode request has been Submitted successfully");

        if ( job == null )
        {
            String errMsg = "Can not find the workflow with step name " + stepName;
            LOGGER.error( errMsg);
            throw new WorkflowNotFoundException(HttpStatus.NOT_FOUND.value(), errMsg);
        }

        orchestrationService.orchestrateWorkflow(job, addNodeService);

        return new ResponseEntity<>(addNodeService.makeNodeExpansionResponse(job), HttpStatus.OK);
    }

    /**
     * This returns the workflow job information for the specified job identifier.
     * 
     * @param   jobId           The job identifier.
     * @param   servletRequest  The servlet request.
     * 
     * @return  The node expansion response entity.
     * 
     * @since   1.0
     */
    @CrossOrigin
    @RequestMapping(path = "/nodes/{jobId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<NodeExpansionResponse> getAddNodeJob(
            final @PathVariable UUID jobId, final HttpServletRequest servletRequest)
        throws WorkflowNotFoundException
    {
        final Job job = addNodeService.findJob(jobId);

        if ( job == null ) {
            String errMsg = "Can not find the workflow with jobId " + jobId;
            LOGGER.error(errMsg);
            throw new WorkflowNotFoundException(HttpStatus.NOT_FOUND.value(), errMsg);
        }

        return new ResponseEntity<>(addNodeService.makeNodeExpansionResponse(job), HttpStatus.OK);


    }

    @CrossOrigin
    @RequestMapping(path = "/preprocess/{jobId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<NodeExpansionResponse> getPreProcessJob(
            final @PathVariable UUID jobId, final HttpServletRequest servletRequest)
            throws WorkflowNotFoundException
    {
        final Job job = preProcessService.findJob(jobId);

        if ( job == null )
        {
            String errMsg = "Can not find the workflow with jobId " + jobId;
            LOGGER.error(errMsg);
            throw new WorkflowNotFoundException(HttpStatus.NOT_FOUND.value(), errMsg);
        }

        return new ResponseEntity<>(preProcessService.makeNodeExpansionResponse(job), HttpStatus.OK);

    }

    public void setWorkflowService(WorkflowService workflowService){
        addNodeService.setWorkflowService(workflowService);
    }

    @RequestMapping(path = "/clusters", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<ClusterInfo> listVirtualizationClusters(HttpServletRequest servletRequest) throws ServiceTimeoutException, ServiceExecutionException {

        List<ClusterInfo> clusters = nodeService.listClusters();
        if (clusters != null )
        {
            LOGGER.info("Return " + clusters.size() + " clusters information.");
            return clusters;
        }

        return new ArrayList<>();
    }

    //to hide the exception class name from http response
    @ExceptionHandler(WorkflowNotFoundException.class)
    void handleWorkflowNotFoundException(WorkflowNotFoundException e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setAttribute(DefaultErrorAttributes.class.getName() + ".ERROR", null);
        response.sendError(e.getCode(), e.getMessage());
    }

    //to hide the exception class name from http response
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request, HttpServletResponse response) throws IOException{
        String errMsg = "Can not find the JobId - "+e.getValue();
        LOGGER.error(errMsg);
        request.setAttribute(DefaultErrorAttributes.class.getName() + ".ERROR", null);
        response.sendError(HttpStatus.NOT_FOUND.value(), errMsg);
    }

    @RequestMapping(path = "/idrackNetworkSettings", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IdracInfo setIdracNetworkSettings(@RequestBody IdracNetworkSettingsRequest idracNetworkSettingsRequest,
                                             HttpServletRequest servletRequest) throws ServiceTimeoutException, ServiceExecutionException {

        IdracInfo idracInfo = nodeService.idracNetworkSettings(idracNetworkSettingsRequest);
        if (idracInfo != null )
        {
            LOGGER.info("Return idracInfo " + idracInfo);
            return idracInfo;
        }

        return idracInfo;
    }
}
