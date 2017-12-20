/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InventoryNodeTest
{
    @Mock
    private NodeService nodeService;

    @Mock
    private DataServiceRepository dataServiceRepository;

    @Mock
    private DelegateExecution delegateExecution;

    private InventoryNode inventoryNode;

    private NodeDetail nodeDetail;
    private String nodeInventoryResponse = "TestResponse";

    @Before
    public void setUp() throws Exception
    {
        inventoryNode = spy(new InventoryNode(nodeService, dataServiceRepository));

        nodeDetail = new NodeDetail("1", "abc");

        doReturn(nodeDetail).when(delegateExecution).getVariable(NODE_DETAIL);

        doReturn(nodeInventoryResponse).when(nodeService).listNodeInventory("1");

        doReturn(true).when(dataServiceRepository).saveNodeInventory(any());

    }

    @Ignore
    @Test
    public void testSuccessful() throws Exception
    {
        inventoryNode.delegateExecute(delegateExecution);
    }

    @Ignore
    @Test
    public void testServiceExecutionException() throws Exception
    {
        Exception e = new ServiceExecutionException("ServiceExecutionException");
        doThrow(e).when(nodeService).listNodeInventory("1");
        inventoryNode.delegateExecute(delegateExecution);
        assertTrue(nodeDetail.isInventoryFailed());
        verify(inventoryNode).updateDelegateStatus("Requesting Update Node Inventory for Node abc");
        verify(inventoryNode).updateDelegateStatus("Update Node Inventory request failed for Node abc", e);
    }

    @Ignore
    @Test
    public void testServiceTimeoutException() throws Exception
    {
        Exception e = new ServiceExecutionException("ServiceExecutionException");
        doThrow(e).when(nodeService).listNodeInventory("1");
        inventoryNode.delegateExecute(delegateExecution);
        assertTrue(nodeDetail.isInventoryFailed());
        verify(inventoryNode).updateDelegateStatus("Requesting Update Node Inventory for Node abc");
        verify(inventoryNode).updateDelegateStatus("Update Node Inventory request failed for Node abc", e);

    }

    @Ignore
    @Test
    public void testNodeInventorySaveFailed() throws Exception
    {
        doReturn(false).when(dataServiceRepository).saveNodeInventory(any());
        inventoryNode.delegateExecute(delegateExecution);
        assertTrue(nodeDetail.isInventoryFailed());
        verify(inventoryNode).updateDelegateStatus("Requesting Update Node Inventory for Node abc");
        verify(inventoryNode).updateDelegateStatus("Update Node Inventory on Node abc failed.");
    }
}
