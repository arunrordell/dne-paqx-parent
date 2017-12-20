package com.dell.cpsd.paqx.dne.service.task.handler.preprocess;

import com.dell.cpsd.paqx.dne.domain.Job;
import com.dell.cpsd.paqx.dne.domain.node.NodeInventory;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.model.NodeExpansionRequest;
import com.dell.cpsd.paqx.dne.service.model.Status;
import com.dell.cpsd.paqx.dne.service.model.TaskResponse;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import com.dell.cpsd.service.common.client.exception.ServiceTimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DiscoverNodeInventoryTaskHandlerTest
{

    @Mock
    private NodeService nodeService;

    @Mock
    private DataServiceRepository repository;

    @Mock
    private TaskResponse taskResponse;

    @Mock
    private Job job;

    @Mock
    private NodeExpansionRequest inputParams;

    private DiscoverNodeInventoryTaskHandler handler;


    @Before
    public void setUp() throws JsonProcessingException {
        this.handler = spy(new DiscoverNodeInventoryTaskHandler(nodeService, repository));
    }

    @Ignore
    @Test
    public void executeTaskSuccessful() throws ServiceTimeoutException, ServiceExecutionException, JsonProcessingException{
        doReturn(this.taskResponse).when(this.handler).initializeResponse(this.job);
        doReturn(this.inputParams).when(this.job).getInputParams();
        when(this.inputParams.getSymphonyUuid()).thenReturn(UUID.randomUUID().toString());
        doReturn(this.buildNodeInventory()).when(this.nodeService).listNodeInventory(anyString());
        doReturn(true).when(this.repository).saveNodeInventory(any());

        boolean result = handler.executeTask(job);

        assertThat(result, is(true));
        verify(this.taskResponse).setWorkFlowTaskStatus(Status.SUCCEEDED);
        verify(this.taskResponse, never()).addError(anyString());
    }

    @Ignore
    @Test
    public void executeTaskFailureNodeInventorySaveFailure() throws ServiceTimeoutException, ServiceExecutionException, JsonProcessingException{
        doReturn(this.taskResponse).when(this.handler).initializeResponse(this.job);
        doReturn(this.inputParams).when(this.job).getInputParams();
        when(this.inputParams.getSymphonyUuid()).thenReturn(UUID.randomUUID().toString());
        doReturn(this.buildNodeInventory()).when(this.nodeService).listNodeInventory(anyString());
        doReturn(false).when(this.repository).saveNodeInventory(any());

        boolean result = handler.executeTask(job);

        assertThat(result, is(false));
        verify(this.taskResponse).setWorkFlowTaskStatus(Status.FAILED);
        verify(this.taskResponse, never()).addError(anyString());
    }

    @Test
    public void executeTaskFailureWithNullNodeInventory() throws ServiceTimeoutException, ServiceExecutionException, JsonProcessingException{
        doReturn(this.taskResponse).when(this.handler).initializeResponse(this.job);
        doReturn(this.inputParams).when(this.job).getInputParams();
        when(this.inputParams.getSymphonyUuid()).thenReturn(UUID.randomUUID().toString());
        doReturn(null).when(this.nodeService).listNodeInventory(anyString());

        boolean result = handler.executeTask(job);

        assertThat(result, is(false));
        verify(this.taskResponse).setWorkFlowTaskStatus(Status.FAILED);
        verify(this.taskResponse).addError(anyString());
    }

    @Test
    public void executeTaskFailureException() throws ServiceTimeoutException, ServiceExecutionException, JsonProcessingException{
        doReturn(this.taskResponse).when(this.handler).initializeResponse(this.job);
        doReturn(this.inputParams).when(this.job).getInputParams();
        when(this.inputParams.getSymphonyUuid()).thenReturn(UUID.randomUUID().toString());
        when(this.nodeService.listNodeInventory(anyString())).thenThrow(ServiceTimeoutException.class);

        boolean result = handler.executeTask(job);

        assertThat(result, is(false));
        verify(this.taskResponse).setWorkFlowTaskStatus(Status.FAILED);
        verify(this.taskResponse).addError(anyString());
    }

    private Object buildNodeInventory()
    {
        String DMI_FIELD = "dmi";
        String SOURCE_FIELD = "source";
        String DATA_FIELD = "data";
        String SYSTEM_INFO_FIELD = "System Information";
        String SERIAL_NUM_FIELD = "Serial Number";
        String PRODUCT_FIELD = "Product Name";
        String FAMILY_FIELD = "Family";

        Map<String, String> sysInfoData = new HashMap<>();
        sysInfoData.put(SERIAL_NUM_FIELD, "ABCDEFG");
        sysInfoData.put(PRODUCT_FIELD, "DellNode730");
        sysInfoData.put(FAMILY_FIELD, "family");
        Map<String, Map<String, String>> sysInfo = new HashMap<>();
        sysInfo.put(SYSTEM_INFO_FIELD, sysInfoData);

        Map<String, Object> source = new HashMap<>();
        source.put(DATA_FIELD, sysInfo);
        source.put(SOURCE_FIELD, DMI_FIELD);
        List<Object> retList = new ArrayList<>();
        retList.add(source);
        return retList;
    }
}
