/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotifyNodeDiscoveryToUpdateStatusCompletedTest
{
    private NotifyNodeDiscoveryToUpdateStatusCompleted notifyNodeDiscoveryToUpdateStatusCompleted;
    private List<NodeDetail>                           completedNodeDetails;
    private NodeService                                nodeService;
    private DelegateExecution                          delegateExecution;

    @Before
    public void setUp() throws Exception
    {
        nodeService = mock(NodeService.class);
        delegateExecution = mock(DelegateExecution.class);

        notifyNodeDiscoveryToUpdateStatusCompleted = new NotifyNodeDiscoveryToUpdateStatusCompleted(nodeService);

        NodeDetail nodeDetail = new NodeDetail();
        nodeDetail.setId("abc");
        nodeDetail.setServiceTag("abc");
        nodeDetail.setCompleted(true);

        completedNodeDetails = new ArrayList<>();
        completedNodeDetails.add(nodeDetail);
    }

    @Test
    public void testNodeStatusChangeFailed() throws Exception
    {
        try
        {
            when(delegateExecution.getVariable(DelegateConstants.COMPLETED_NODE_DETAILS)).thenReturn(completedNodeDetails);
            when(nodeService.notifyNodeAllocationStatus(anyString(), anyString())).thenReturn(false);

            notifyNodeDiscoveryToUpdateStatusCompleted.delegateExecute(delegateExecution);

            fail("Should be an exception.");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.NOTIFY_NODE_STATUS_COMPLETED_FAILED));
            assertThat(error.getMessage(), containsString("Updating Node Status to completed failed"));
        }
    }

    @Test
    public void testGeneralException() throws Exception
    {
        try
        {
            when(delegateExecution.getVariable(DelegateConstants.COMPLETED_NODE_DETAILS)).thenReturn(completedNodeDetails);
            when(nodeService.notifyNodeAllocationStatus(anyString(), anyString())).thenThrow(new NullPointerException());

            notifyNodeDiscoveryToUpdateStatusCompleted.delegateExecute(delegateExecution);

            fail("Should be an exception.");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.NOTIFY_NODE_STATUS_COMPLETED_FAILED));
            assertThat(error.getMessage(), containsString("Updating Node Status to completed failed"));
        }
    }

    @Test
    public void testNodeStatusChangeSuccess() throws Exception
    {
        when(delegateExecution.getVariable(DelegateConstants.COMPLETED_NODE_DETAILS)).thenReturn(completedNodeDetails);
        when(nodeService.notifyNodeAllocationStatus(anyString(), anyString())).thenReturn(true);
        final NotifyNodeDiscoveryToUpdateStatusCompleted nus = spy(new NotifyNodeDiscoveryToUpdateStatusCompleted(nodeService));

        nus.delegateExecute(delegateExecution);

        ArgumentCaptor<String> updateDelegateStatusCaptor = ArgumentCaptor.forClass(String.class);
        verify(nus, times(2)).updateDelegateStatus(updateDelegateStatusCaptor.capture());
        assertThat(updateDelegateStatusCaptor.getValue(), CoreMatchers.containsString("was successful"));

        ArgumentCaptor<String> notifyNodeAllocationStatusCaptor = ArgumentCaptor.forClass(String.class);
        verify(nodeService).notifyNodeAllocationStatus(anyString(), notifyNodeAllocationStatusCaptor.capture());
        assertEquals(DelegateConstants.COMPLETED, notifyNodeAllocationStatusCaptor.getValue());
    }
}
