/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.delegates.request;

import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import com.dell.cpsd.sdk.AMQPClient;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendAddNodesToSystemDefinitionTest
{
    @Mock
    private RuntimeService runtimeService;

    @Mock
    private AMQPClient sdkAMQPClient;

    @Mock
    private DataServiceRepository repository;

    @Mock
    private ExecutorService dneTaskExecutorService;

    @Mock
    private DelegateExecution delegateExecution;

    private SendAddNodesToSystemDefinition sendAddNodesToSystemDefinition;
    private NodeDetail                     nodeDetail;
    private List<NodeDetail>               completedNodeDetails;

    @Before
    public void setUp() throws Exception
    {
        sendAddNodesToSystemDefinition = new SendAddNodesToSystemDefinition(runtimeService, sdkAMQPClient, repository,
                dneTaskExecutorService);

        nodeDetail = new NodeDetail();
        nodeDetail.setServiceTag("abc");
        nodeDetail.setCompleted(true);

        completedNodeDetails = new ArrayList<>();
        completedNodeDetails.add(nodeDetail);
    }

    @Test
    public void testDelegateExecuteException() throws Exception
    {
        when(delegateExecution.getVariable(anyString())).thenReturn(completedNodeDetails);
        doThrow(new NullPointerException()).when(dneTaskExecutorService).execute(any());

        try
        {
            sendAddNodesToSystemDefinition.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown here but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.SEND_ADD_NODES_TO_SYSTEM_DEFINITION_FAILED));
            assertThat(error.getMessage(), containsString("failed"));
        }

    }

    @Test
    public void testDelegateExecuteNoCompletedNodeDetailsFound() throws Exception
    {
        when(delegateExecution.getVariable(anyString())).thenReturn(Collections.emptyList());

        try
        {
            sendAddNodesToSystemDefinition.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown here but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.SEND_ADD_NODES_TO_SYSTEM_DEFINITION_FAILED));
            assertThat(error.getMessage(), containsString("no completed nodes"));
        }
    }

    @Test
    public void testDelegateExecuteSuccess() throws Exception
    {
        when(delegateExecution.getVariable(anyString())).thenReturn(completedNodeDetails);
        final SendAddNodesToSystemDefinition sendAddNodesToSystemDefinitionSpy = spy(sendAddNodesToSystemDefinition);

        sendAddNodesToSystemDefinitionSpy.delegateExecute(delegateExecution);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sendAddNodesToSystemDefinitionSpy, times(2)).updateDelegateStatus(captor.capture());
        assertThat(captor.getValue(), containsString("was successful"));
    }
}