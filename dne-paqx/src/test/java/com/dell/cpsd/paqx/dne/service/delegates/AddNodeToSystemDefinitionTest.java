/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import com.dell.cpsd.sdk.AMQPClient;
import com.dell.cpsd.service.system.definition.api.Component;
import com.dell.cpsd.service.system.definition.api.ConvergedSystem;
import com.dell.cpsd.service.system.definition.api.Group;
import com.dell.cpsd.service.system.definition.api.Identity;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AddNodeToSystemDefinitionTest
{
    @Mock
    private AMQPClient sdkAMQPClient;

    @Mock
    private DataServiceRepository repository;

    @Mock
    private DelegateExecution delegateExecution;

    private AddNodeToSystemDefinition addNodeToSystemDefinition;
    private NodeDetail                nodeDetail;
    private ConvergedSystem           convergedSystem;
    private DiscoveredNodeInfo        discoveredNodeInfo;
    private Group                     systemComputeGroup;

    @Before
    public void setUp() throws Exception
    {
        addNodeToSystemDefinition = new AddNodeToSystemDefinition(sdkAMQPClient, repository);

        nodeDetail = new NodeDetail();
        nodeDetail.setId("123456789");
        nodeDetail.setServiceTag("abc");

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
        when(delegateExecution.getVariable(NODE_DETAIL)).thenReturn(nodeDetail);
        when(sdkAMQPClient.getConvergedSystems()).thenReturn(Collections.emptyList());

        try
        {
            addNodeToSystemDefinition.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.ADD_NODES_TO_SYSTEM_DEFINITION_FAILED));
            assertThat(error.getMessage(), containsString("No converged systems found"));
        }
    }

    @Test
    public void testNoConvergedSystemFoundException() throws Exception
    {
        when(delegateExecution.getVariable(NODE_DETAIL)).thenReturn(nodeDetail);
        when(sdkAMQPClient.getConvergedSystems()).thenReturn(Collections.singletonList(convergedSystem));
        when(sdkAMQPClient.getComponents(any())).thenReturn(Collections.emptyList());

        try
        {
            addNodeToSystemDefinition.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.ADD_NODES_TO_SYSTEM_DEFINITION_FAILED));
            assertThat(error.getMessage(), containsString("No converged system found"));
        }
    }

    @Test
    public void testNoDiscoveredNodeInfoException() throws Exception
    {
        when(delegateExecution.getVariable(NODE_DETAIL)).thenReturn(nodeDetail);
        when(sdkAMQPClient.getConvergedSystems()).thenReturn(Collections.singletonList(convergedSystem));
        when(sdkAMQPClient.getComponents(any())).thenReturn(Collections.singletonList(convergedSystem));
        when(repository.getDiscoveredNodeInfo(anyString())).thenReturn(null);

        try
        {
            addNodeToSystemDefinition.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.ADD_NODES_TO_SYSTEM_DEFINITION_FAILED));
            assertThat(error.getMessage(), containsString("No discovered node info"));
        }
    }

    @Test
    public void testNodeNotAddedToSystemDefinitionException() throws Exception
    {
        when(delegateExecution.getVariable(NODE_DETAIL)).thenReturn(nodeDetail);
        when(sdkAMQPClient.getConvergedSystems()).thenReturn(Collections.singletonList(convergedSystem));
        when(sdkAMQPClient.getComponents(any())).thenReturn(Collections.singletonList(convergedSystem));
        when(repository.getDiscoveredNodeInfo(anyString())).thenReturn(discoveredNodeInfo);

        try
        {
            addNodeToSystemDefinition.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.ADD_NODES_TO_SYSTEM_DEFINITION_FAILED));
            assertThat(error.getMessage(), containsString("node was not added to the system definition"));
        }
    }

    @Test
    public void testGeneralException() throws Exception
    {
        when(delegateExecution.getVariable(NODE_DETAIL)).thenReturn(nodeDetail);
        when(sdkAMQPClient.getConvergedSystems()).thenThrow(new NullPointerException());

        try
        {
            addNodeToSystemDefinition.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.ADD_NODES_TO_SYSTEM_DEFINITION_FAILED));
            assertThat(error.getMessage(), containsString("failed"));
        }
    }

    @Test
    public void testSuccess() throws Exception
    {
        when(delegateExecution.getVariable(NODE_DETAIL)).thenReturn(nodeDetail);
        when(sdkAMQPClient.getConvergedSystems()).thenReturn(Collections.singletonList(convergedSystem));
        when(sdkAMQPClient.getComponents(any())).thenReturn(Collections.singletonList(convergedSystem));
        when(repository.getDiscoveredNodeInfo(anyString())).thenReturn(discoveredNodeInfo);
        AddNodeToSystemDefinition addNodeToSystemDefinitionSpy = spy(addNodeToSystemDefinition);

        doAnswer(invocation -> {
            final Component addedComponent = new Component();
            addedComponent.setIdentity(new Identity("elementType", discoveredNodeInfo.getSymphonyUuid(), "address", "serialNumber", null));
            convergedSystem.setComponents(Collections.singletonList(addedComponent));
            return null;
        }).when(sdkAMQPClient).addComponent(any(), any(), anyList(), anyString());

        addNodeToSystemDefinitionSpy.delegateExecute(delegateExecution);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(addNodeToSystemDefinitionSpy, times(2)).updateDelegateStatus(captor.capture());
        assertThat(captor.getValue(), containsString("was successful"));
    }
}