/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.HOSTNAME;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.INSTALL_ESXI_MESSAGE_ID;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InstallEsxiTest
{
    private InstallEsxi installEsxi;
    private NodeDetail  nodeDetail;

    @Mock
    private AsynchronousNodeService asynchronousNodeService;

    @Mock
    private DataServiceRepository repository;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private AsynchronousNodeServiceCallback<?> asynchronousNodeServiceCallback;

    @Before
    public void setUp()
    {
        installEsxi = new InstallEsxi(asynchronousNodeService, repository);

        doReturn(true).when(asynchronousNodeServiceCallback).isDone();

        doReturn("domain").when(this.repository).getDomainName();

        nodeDetail = new NodeDetail();
        nodeDetail.setId("1");
        nodeDetail.setIdracIpAddress("1");
        nodeDetail.setIdracGatewayIpAddress("1");
        nodeDetail.setIdracSubnetMask("1");
        nodeDetail.setServiceTag("abc");
        nodeDetail.setEsxiManagementHostname("hostName");
        doReturn(nodeDetail).when(delegateExecution).getVariable(NODE_DETAIL);

        doReturn(asynchronousNodeServiceCallback).when(this.delegateExecution).getVariable(INSTALL_ESXI_MESSAGE_ID);
    }

    @Test
    public void testSuccess() throws Exception
    {
        doReturn("succeeded").when(asynchronousNodeService).requestInstallEsxi(asynchronousNodeServiceCallback);
        installEsxi.delegateExecute(delegateExecution);
        verify(delegateExecution, times(1)).setVariable(HOSTNAME, "hostName.domain");
    }

    @Test
    public void testException() throws Exception
    {
        doThrow(new ServiceExecutionException("Error1")).when(asynchronousNodeService).requestInstallEsxi(asynchronousNodeServiceCallback);
        try
        {
            installEsxi.delegateExecute(delegateExecution);
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.INSTALL_ESXI_FAILED));
            assertTrue((error.getMessage().equals("Install Esxi on Node " + nodeDetail.getServiceTag() + " failed!  Reason: Error1")));
        }
    }

    @Test
    public void testFailed() throws Exception
    {
        doReturn("failed").when(asynchronousNodeService).requestInstallEsxi(asynchronousNodeServiceCallback);
        try
        {
            installEsxi.delegateExecute(delegateExecution);
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.INSTALL_ESXI_FAILED));
            assertTrue((error.getMessage().equals("Install Esxi on Node abc failed!")));
        }
    }

}
