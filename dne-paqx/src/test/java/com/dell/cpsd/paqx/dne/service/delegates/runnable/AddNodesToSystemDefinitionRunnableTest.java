/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.delegates.runnable;

import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import com.dell.cpsd.sdk.AMQPClient;
import com.dell.cpsd.service.system.definition.api.ConvergedSystem;
import com.dell.cpsd.service.system.definition.api.Group;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ExecutionQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AddNodesToSystemDefinitionRunnableTest
{
    @Mock
    private AMQPClient sdkAMQPClient;

    @Mock
    private DataServiceRepository repository;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ExecutionQuery executionQuery;

    @Mock
    private Execution execution;

    private AddNodesToSystemDefinitionRunnable addNodesToSystemDefinitionRunnable;
    private NodeDetail                         nodeDetail;
    private List<NodeDetail>                   completedNodeDetails;
    private ConvergedSystem                    convergedSystem;
    private DiscoveredNodeInfo                 discoveredNodeInfo;
    private Group                              systemComputeGroup;

    private String executionId = "executionId";

    @Before
    public void setUp() throws Exception
    {
        nodeDetail = new NodeDetail();
        nodeDetail.setId("123456789");
        nodeDetail.setServiceTag("abc");
        nodeDetail.setCompleted(true);

        completedNodeDetails = new ArrayList<>();
        completedNodeDetails.add(nodeDetail);

        addNodesToSystemDefinitionRunnable = new AddNodesToSystemDefinitionRunnable("processId", "activityId", "messageId",
                completedNodeDetails, runtimeService, sdkAMQPClient, repository);

        convergedSystem = new ConvergedSystem();
        convergedSystem.setUuid("cs-uuid-1");

        discoveredNodeInfo = new DiscoveredNodeInfo("model", "modelFamily", "product", "productFamily", "serialNumber", "symphonyUUID");

        systemComputeGroup = new Group();
        systemComputeGroup.setName("SystemCompute");
        systemComputeGroup.setUuid("group-uuid-1");

        convergedSystem.setGroups(Collections.singletonList(systemComputeGroup));
    }

    @Test
    public void testNoConvergedSystemsFoundException() throws Exception
    {
        when(sdkAMQPClient.getConvergedSystems()).thenReturn(Collections.emptyList());
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.processInstanceId(anyString())).thenReturn(executionQuery);
        when(executionQuery.activityId(anyString())).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(execution);
        when(execution.getId()).thenReturn(executionId);

        addNodesToSystemDefinitionRunnable.run();

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(runtimeService).setVariable(anyString(), anyString(), resultCaptor.capture());
        assertThat(resultCaptor.getValue(), containsString("No converged systems found"));
    }

    @Test
    public void testNoConvergedSystemFoundException() throws Exception
    {
        when(sdkAMQPClient.getConvergedSystems()).thenReturn(Collections.singletonList(convergedSystem));
        when(sdkAMQPClient.getComponents(any())).thenReturn(Collections.emptyList());
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.processInstanceId(anyString())).thenReturn(executionQuery);
        when(executionQuery.activityId(anyString())).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(execution);
        when(execution.getId()).thenReturn(executionId);

        addNodesToSystemDefinitionRunnable.run();

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(runtimeService).setVariable(anyString(), anyString(), resultCaptor.capture());
        assertThat(resultCaptor.getValue(), containsString("No converged system found"));
    }

    @Test
    public void testNoDiscoveredNodeInfoException() throws Exception
    {
        when(sdkAMQPClient.getConvergedSystems()).thenReturn(Collections.singletonList(convergedSystem));
        when(sdkAMQPClient.getComponents(any())).thenReturn(Collections.singletonList(convergedSystem));
        when(repository.getDiscoveredNodeInfo(anyString())).thenReturn(null);
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.processInstanceId(anyString())).thenReturn(executionQuery);
        when(executionQuery.activityId(anyString())).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(execution);
        when(execution.getId()).thenReturn(executionId);

        addNodesToSystemDefinitionRunnable.run();

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(runtimeService).setVariable(anyString(), anyString(), resultCaptor.capture());
        assertThat(resultCaptor.getValue(), containsString("No discovered node info"));
    }

    @Test
    public void testGeneralException() throws Exception
    {
        when(sdkAMQPClient.getConvergedSystems()).thenThrow(new NullPointerException());
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.processInstanceId(anyString())).thenReturn(executionQuery);
        when(executionQuery.activityId(anyString())).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(execution);
        when(execution.getId()).thenReturn(executionId);

        addNodesToSystemDefinitionRunnable.run();

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(runtimeService).setVariable(anyString(), anyString(), resultCaptor.capture());
        assertThat(resultCaptor.getValue(), containsString("failed"));
    }

    @Test
    public void testSuccess() throws Exception
    {
        when(sdkAMQPClient.getConvergedSystems()).thenReturn(Collections.singletonList(convergedSystem));
        when(sdkAMQPClient.getComponents(any())).thenReturn(Collections.singletonList(convergedSystem));
        when(sdkAMQPClient.addComponentToConvergedSystem(any(), any(), anyList(), anyString())).thenReturn(Collections.emptyList());
        when(repository.getDiscoveredNodeInfo(anyString())).thenReturn(discoveredNodeInfo);
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.processInstanceId(anyString())).thenReturn(executionQuery);
        when(executionQuery.activityId(anyString())).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(execution);
        when(execution.getId()).thenReturn(executionId);

        addNodesToSystemDefinitionRunnable.run();

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        verify(runtimeService).setVariable(anyString(), anyString(), resultCaptor.capture());
        assertThat(resultCaptor.getValue(), containsString(DelegateConstants.SUCCEEDED));
    }
}