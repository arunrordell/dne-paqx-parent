/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.task.handler.addnode;

import com.dell.cpsd.paqx.dne.domain.Job;
import com.dell.cpsd.paqx.dne.domain.WorkflowTask;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds;
import com.dell.cpsd.paqx.dne.service.model.DatastoreRenameTaskResponse;
import com.dell.cpsd.paqx.dne.service.model.NodeExpansionRequest;
import com.dell.cpsd.paqx.dne.service.model.Status;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * The tests for the AddNodeToSystemDefinitionTaskHandler class.
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DatastoreRenameTaskHandlerTest
{
    @Mock
    private WorkflowTask task;

    @Mock
    private Job job;

    @Mock
    private DatastoreRenameTaskResponse response;

    @Mock
    private NodeService service;

    @Mock
    private DataServiceRepository repository;

    @Mock
    private NodeExpansionRequest request;

    @Mock
    private ComponentEndpointIds componentEndpointIds;

    private DatastoreRenameTaskHandler handler;
    private String taskName = "datastoreRenameTask";
    private String stepName = "datastoreRenameStep";
    private final String esxiManagementHostname = "fpr1-h17";
    private final String dataStorweName = "DAS17";

    @Before
    public void setUp() throws Exception
    {
        this.handler = spy(new DatastoreRenameTaskHandler(this.service, repository));
    }

    @Test
    public void executeTask_should_successfully_request_a_datastore_rename() throws Exception
    {
        doReturn(this.response).when(this.handler).initializeResponse(any());
        doReturn(this.request).when(this.job).getInputParams();
        doReturn(this.componentEndpointIds).when(this.repository).getVCenterComponentEndpointIdsByEndpointType(anyString());
        doReturn(this.esxiManagementHostname).when(this.request).getEsxiManagementHostname();
        doReturn(true).when(this.service).requestDatastoreRename(any());

        boolean result = this.handler.executeTask(this.job);

        assertThat(result, is(true));
        verify(this.response).setWorkFlowTaskStatus(Status.SUCCEEDED);
        verify(this.response, never()).addError(anyString());
        verify(this.response).setDatastoreName(this.dataStorweName);
    }

    @Test
    public void executeTask_should_fail_the_workflow_when_the_datastore_rename_request_fails() throws Exception
    {
        doReturn(this.response).when(this.handler).initializeResponse(any());
        doReturn(this.request).when(this.job).getInputParams();
        doReturn(this.componentEndpointIds).when(this.repository).getVCenterComponentEndpointIdsByEndpointType(anyString());
        doReturn(this.esxiManagementHostname).when(this.request).getEsxiManagementHostname();
        doReturn(false).when(this.service).requestDatastoreRename(any());

        boolean result = this.handler.executeTask(this.job);

        assertThat(result, is(false));
        verify(this.response).setWorkFlowTaskStatus(Status.FAILED);
        verify(this.response).addError(anyString());
        verify(this.response, never()).setDatastoreName(anyString());
    }

    @Test
    public void executeTask_should_fail_the_workflow_when_the_job_input_params_is_null() throws Exception
    {
        doReturn(this.response).when(this.handler).initializeResponse(any());
        doReturn(null).when(this.job).getInputParams();

        boolean result = this.handler.executeTask(this.job);

        assertThat(result, is(false));
        verify(this.response).setWorkFlowTaskStatus(Status.FAILED);
        verify(this.response).addError(anyString());
        verify(this.response, never()).setDatastoreName(anyString());
    }

    @Test
    public void executeTask_should_fail_the_workflow_when_the_component_endpoint_ids_are_null() throws Exception
    {
        doReturn(this.response).when(this.handler).initializeResponse(any());
        doReturn(this.request).when(this.job).getInputParams();
        doReturn(null).when(this.repository).getVCenterComponentEndpointIdsByEndpointType(anyString());

        boolean result = this.handler.executeTask(this.job);

        assertThat(result, is(false));
        verify(this.response).setWorkFlowTaskStatus(Status.FAILED);
        verify(this.response).addError(anyString());
        verify(this.response, never()).setDatastoreName(anyString());
    }

    @Test
    public void executeTask_should_fail_the_workflow_when_the_esxi_management_hostname_is_null() throws Exception
    {
        doReturn(this.response).when(this.handler).initializeResponse(any());
        doReturn(this.request).when(this.job).getInputParams();
        doReturn(this.componentEndpointIds).when(this.repository).getVCenterComponentEndpointIdsByEndpointType(anyString());
        doReturn(null).when(this.request).getEsxiManagementHostname();

        boolean result = this.handler.executeTask(this.job);

        assertThat(result, is(false));
        verify(this.response).setWorkFlowTaskStatus(Status.FAILED);
        verify(this.response).addError(anyString());
        verify(this.response, never()).setDatastoreName(anyString());
    }

    @Test
    public void testInitializeResponse_should_successfully_create_the_response()
    {
        doReturn(this.task).when(this.job).getCurrentTask();
        doReturn(this.taskName).when(this.task).getTaskName();
        doReturn(this.stepName).when(this.job).getStep();

        DatastoreRenameTaskResponse response = this.handler.initializeResponse(this.job);

        assertNotNull(response);
        assertEquals(this.taskName, response.getWorkFlowTaskName());
        assertEquals(Status.IN_PROGRESS, response.getWorkFlowTaskStatus());
    }
}