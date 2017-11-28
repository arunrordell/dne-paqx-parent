/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CleanInMemoryDatabaseTest
{
    private CleanInMemoryDatabase cleanInMemoryDatabase;
    private DelegateExecution     delegateExecution;
    private DataServiceRepository repository;

    @Before
    public void setUp() throws Exception
    {
        repository = mock(DataServiceRepository.class);
        cleanInMemoryDatabase = new CleanInMemoryDatabase(repository);
        delegateExecution = mock(DelegateExecution.class);
    }

    @Test
    public void testExceptionThrown1() throws Exception
    {
        try
        {
            given(repository.cleanInMemoryDatabase()).willThrow(new NullPointerException("Test Exception"));
            cleanInMemoryDatabase.delegateExecute(delegateExecution);
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.CLEAN_IN_MEMORY_DATABASE_ERROR));
            assertTrue(error.getMessage().contains("An Unexpected Error occurred while cleaning up the Database. Reason: Test Exception"));
        }
    }

    @Test
    public void testExecutionFailed()
    {
        try
        {
            when(repository.cleanInMemoryDatabase()).thenReturn(false);
            cleanInMemoryDatabase.delegateExecute(delegateExecution);
        }
        catch (BpmnError error)
        {
            assertTrue(error.getErrorCode().equals(DelegateConstants.CLEAN_IN_MEMORY_DATABASE_ERROR));
            assertTrue(error.getMessage().contains("Cleaning up the Database failed."));
        }
    }

    @Test
    public void testSuccess()
    {
        when(repository.cleanInMemoryDatabase()).thenReturn(true);
        final CleanInMemoryDatabase cleanInMemoryDatabaseSpy = spy(cleanInMemoryDatabase);
        cleanInMemoryDatabaseSpy.delegateExecute(delegateExecution);
        verify(cleanInMemoryDatabaseSpy).updateDelegateStatus("Cleaning up the Database was successful.");

    }
}
