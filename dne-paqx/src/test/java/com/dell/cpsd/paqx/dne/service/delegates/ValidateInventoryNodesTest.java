/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.INVENTORY_NODES_FAILED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAILS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ValidateInventoryNodesTest
{

    @Mock
    private DelegateExecution delegateExecution;

    private ValidateInventoryNodes validateInventoryNodes;
    private List<NodeDetail> nodeDetailList;
    private NodeDetail node1;
    private NodeDetail node2;

    @Before
    public void setUp() throws Exception
    {
        validateInventoryNodes = spy(new ValidateInventoryNodes());

        node1 = new NodeDetail("1", "abc");
        node2 = new NodeDetail("2", "def");
        nodeDetailList = new ArrayList<>();
        nodeDetailList.add(node1);
        nodeDetailList.add(node2);
        doReturn(nodeDetailList).when(delegateExecution).getVariable(NODE_DETAILS);
    }

    @Test
    public void testSuccessful() {
        validateInventoryNodes.delegateExecute(delegateExecution);
    }

    @Test
    public void testInventoryFailed() {
        try
        {
            node2.setInventoryFailed(true);
            validateInventoryNodes.delegateExecute(delegateExecution);
            fail("Should Not Get Here");
        } catch( BpmnError bpmnError) {
            assertEquals(bpmnError.getErrorCode(), INVENTORY_NODES_FAILED);
            assertEquals(bpmnError.getMessage(), "Update of Node Inventories was not successful for Nodes def.");
            verify(validateInventoryNodes).updateDelegateStatus("Update of Node Inventories was not successful for Nodes def.");
            verify(delegateExecution).setVariable(NODE_DETAILS, nodeDetailList);
        }
    }
}
