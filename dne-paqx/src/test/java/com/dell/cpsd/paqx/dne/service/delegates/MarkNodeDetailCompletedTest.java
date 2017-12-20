/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
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
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MarkNodeDetailCompletedTest
{
    @Mock
    private RuntimeService runtimeService;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private DelegateExecution superDelegateExecution;

    private MarkNodeDetailCompleted markNodeDetailCompleted;
    private List<NodeDetail>        completedNodeDetails;
    private NodeDetail              nodeDetail;

    @Before
    public void setUp() throws Exception
    {
        markNodeDetailCompleted = new MarkNodeDetailCompleted(runtimeService);

        nodeDetail = new NodeDetail();
        nodeDetail.setId("123456789abc");
        nodeDetail.setServiceTag("abc");

        completedNodeDetails = new ArrayList<>();
    }

    @Test
    public void testDelegateExecuteException() throws Exception
    {
        when(delegateExecution.getVariable(anyString())).thenReturn(nodeDetail);
        when(delegateExecution.getSuperExecution()).thenReturn(superDelegateExecution);
        when(superDelegateExecution.getProcessInstanceId()).thenReturn("process-instance-id-1");
        when(runtimeService.getVariable(anyString(), anyString())).thenThrow(new NullPointerException());

        try
        {
            markNodeDetailCompleted.delegateExecute(delegateExecution);

            fail("Expected an exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.MARK_NODE_DETAIL_COMPLETED_FAILED));
            assertThat(error.getMessage(),
                    containsString("An Unexpected Exception occurred attempting to mark the node status as completed"));
            assertThat(completedNodeDetails, hasSize(0));
        }
    }

    @Test
    public void testDelegateExecuteSuccess() throws Exception
    {
        when(delegateExecution.getVariable(anyString())).thenReturn(nodeDetail);
        when(delegateExecution.getSuperExecution()).thenReturn(superDelegateExecution);
        when(superDelegateExecution.getProcessInstanceId()).thenReturn("process-instance-id-1");
        when(runtimeService.getVariable(anyString(), anyString())).thenReturn(completedNodeDetails);
        final MarkNodeDetailCompleted markNodeDetailCompletedSpy = spy(markNodeDetailCompleted);

        markNodeDetailCompletedSpy.delegateExecute(delegateExecution);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(markNodeDetailCompletedSpy, times(2)).updateDelegateStatus(captor.capture());
        assertThat(captor.getValue(), containsString("was successful"));
        assertTrue(nodeDetail.isCompleted());
        assertThat(completedNodeDetails, hasSize(1));
    }
}