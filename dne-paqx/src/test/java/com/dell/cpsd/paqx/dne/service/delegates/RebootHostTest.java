/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.exception.TaskResponseFailureException;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RebootHostTest
{
    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private AsynchronousNodeService asynchronousNodeService;

    private RebootHost rebootHost;

    @Before
    public void setUp() throws Exception
    {
        rebootHost = new RebootHost(asynchronousNodeService);
    }

    @Test
    public void testTaskResponseFailureException() throws Exception
    {
        final String exceptionMsg = "request failed";

        try
        {
            willThrow(new TaskResponseFailureException(1, exceptionMsg)).given(asynchronousNodeService).processRebootHostResponse(any());

            rebootHost.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.REBOOT_HOST_FAILED));
            assertThat(error.getMessage(), containsString(exceptionMsg));
        }
    }

    @Test
    public void testGeneralException() throws Exception
    {
        try
        {
            rebootHost.delegateExecute(delegateExecution);

            fail("Expected exception to be thrown but was not");
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.REBOOT_HOST_FAILED));
            assertThat(error.getMessage(), containsString("An Unexpected Exception occurred attempting to request Reboot Host"));
        }
    }

    @Test
    public void testSuccess() throws Exception
    {
        final RebootHost rebootHostSpy = spy(new RebootHost(asynchronousNodeService));
        final NodeDetail mockNodeDetail = mock(NodeDetail.class);
        final AsynchronousNodeServiceCallback serviceCallback = mock(AsynchronousNodeServiceCallback.class);

        doNothing().when(asynchronousNodeService).processRebootHostResponse(any());
        when(mockNodeDetail.getServiceTag()).thenReturn("1234");
        when(delegateExecution.getVariable(DelegateConstants.NODE_DETAIL)).thenReturn(mockNodeDetail);
        when(delegateExecution.getVariable(DelegateConstants.REBOOT_HOST_MESSAGE_ID)).thenReturn(serviceCallback);

        rebootHostSpy.delegateExecute(delegateExecution);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(rebootHostSpy).updateDelegateStatus(captor.capture());
        assertThat(captor.getValue(), CoreMatchers.containsString("was successful"));
    }
}
