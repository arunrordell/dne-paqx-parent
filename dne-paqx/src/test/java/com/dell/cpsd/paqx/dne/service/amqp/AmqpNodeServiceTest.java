/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.amqp;

import com.dell.cpsd.ChangeIdracCredentialsResponseMessage;
import com.dell.cpsd.CompleteNodeAllocationResponseMessage;
import com.dell.cpsd.FailNodeAllocationResponseMessage;
import com.dell.cpsd.NodeAllocationInfo;
import com.dell.cpsd.NodeAllocationInfo.AllocationStatus;
import com.dell.cpsd.NodesListed;
import com.dell.cpsd.StartNodeAllocationResponseMessage;
import com.dell.cpsd.paqx.dne.amqp.producer.DneProducer;
import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOData;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOStoragePool;
import com.dell.cpsd.paqx.dne.domain.vcenter.VCenter;
import com.dell.cpsd.paqx.dne.exception.TaskResponseFailureException;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.repository.H2DataRepository;
import com.dell.cpsd.paqx.dne.service.model.ChangeIdracCredentialsResponse;
import com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds;
import com.dell.cpsd.paqx.dne.service.model.DiscoveredNode;
import com.dell.cpsd.paqx.dne.service.model.IdracInfo;
import com.dell.cpsd.paqx.dne.service.model.IdracNetworkSettingsRequest;
import com.dell.cpsd.paqx.dne.transformers.DiscoveryInfoToVCenterDomainTransformer;
import com.dell.cpsd.paqx.dne.transformers.ScaleIORestToScaleIODomainTransformer;
import com.dell.cpsd.paqx.dne.transformers.StoragePoolEssRequestTransformer;
import com.dell.cpsd.rackhd.adapter.model.idrac.IdracNetworkSettingsResponse;
import com.dell.cpsd.rackhd.adapter.model.idrac.IdracNetworkSettingsResponseMessage;
import com.dell.cpsd.service.common.client.callback.ServiceCallback;
import com.dell.cpsd.service.common.client.callback.ServiceError;
import com.dell.cpsd.service.common.client.callback.ServiceResponse;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import com.dell.cpsd.service.common.client.exception.ServiceTimeoutException;
import com.dell.cpsd.service.common.client.rpc.DefaultMessageConsumer;
import com.dell.cpsd.service.common.client.rpc.DelegatingMessageConsumer;
import com.dell.cpsd.service.common.client.rpc.ServiceRequestCallback;
import com.dell.cpsd.service.engineering.standards.EssValidateProtectionDomainsRequestMessage;
import com.dell.cpsd.service.engineering.standards.EssValidateProtectionDomainsResponseMessage;
import com.dell.cpsd.service.engineering.standards.EssValidateStoragePoolRequestMessage;
import com.dell.cpsd.service.engineering.standards.EssValidateStoragePoolResponseMessage;
import com.dell.cpsd.storage.capabilities.api.AddHostToProtectionDomainRequestMessage;
import com.dell.cpsd.storage.capabilities.api.AddHostToProtectionDomainResponseMessage;
import com.dell.cpsd.storage.capabilities.api.CredentialNameId;
import com.dell.cpsd.storage.capabilities.api.ListComponentResponseMessage;
import com.dell.cpsd.storage.capabilities.api.ListStorageResponseMessage;
import com.dell.cpsd.storage.capabilities.api.ScaleIOComponentDetails;
import com.dell.cpsd.storage.capabilities.api.ScaleIOSystemDataRestRep;
import com.dell.cpsd.storage.capabilities.api.ScaleIoEndpointDetails;
import com.dell.cpsd.storage.capabilities.api.SioSdcUpdatePerformanceProfileRequestMessage;
import com.dell.cpsd.storage.capabilities.api.SioSdcUpdatePerformanceProfileResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.AddEsxiHostVSphereLicenseRequest;
import com.dell.cpsd.virtualization.capabilities.api.AddEsxiHostVSphereLicenseResponse;
import com.dell.cpsd.virtualization.capabilities.api.AddHostToDvSwitchRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.AddHostToDvSwitchResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.ClusterInfo;
import com.dell.cpsd.virtualization.capabilities.api.ClusterOperationRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.ClusterOperationResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.ConfigureVmNetworkSettingsRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.ConfigureVmNetworkSettingsResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.Datacenter;
import com.dell.cpsd.virtualization.capabilities.api.DatastoreRenameRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.DatastoreRenameResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.DeployVMFromTemplateRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.DeployVMFromTemplateResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.DiscoverClusterResponseInfo;
import com.dell.cpsd.virtualization.capabilities.api.DiscoverClusterResponseInfoMessage;
import com.dell.cpsd.virtualization.capabilities.api.DiscoveryResponseInfoMessage;
import com.dell.cpsd.virtualization.capabilities.api.EnablePCIPassthroughRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.EnablePCIPassthroughResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.HostMaintenanceModeRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.HostMaintenanceModeResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.ListComponentsResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.ListEsxiCredentialDetailsRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.ListEsxiCredentialDetailsResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.MessageProperties;
import com.dell.cpsd.virtualization.capabilities.api.RemoteCommandExecutionRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.RemoteCommandExecutionResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.SoftwareVIBConfigureRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.SoftwareVIBRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.SoftwareVIBResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.UpdatePCIPassthruSVMRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.UpdatePCIPassthruSVMResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.VCenterComponentDetails;
import com.dell.cpsd.virtualization.capabilities.api.VCenterCredentialDetails;
import com.dell.cpsd.virtualization.capabilities.api.VCenterEndpointDetails;
import com.dell.cpsd.virtualization.capabilities.api.VCenterUpdateSoftwareAcceptanceRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.VCenterUpdateSoftwareAcceptanceResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.VmPowerOperationsRequestMessage;
import com.dell.cpsd.virtualization.capabilities.api.VmPowerOperationsResponseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.ADD_HOST_TO_DV_SWITCH;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.ADD_HOST_TO_VCENTER_CLUSTER;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.ADD_SDS_NODE_TO_PROTECTION_DOMAIN;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.APPLY_ESXI_HOST_LICENSE;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.CONFIGURE_PCI_PASSTHROUGH_SCALEIO_VM;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.CONFIGURE_SDC_VIB;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.CONFIGURE_VM_NETWORK_SETTINGS;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.DATASTORE_RENAME;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.DEPLOY_VM_FROM_TEMPLATE;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.DISCOVER_SCALEIO;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.DISCOVER_VCENTER;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.ENABLE_PCI_PASSTHROUGH;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.HOST_MAINTENANCE_MODE;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.INSTALL_SDC_VIB;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.LIST_ESXI_DEFAULT_CREDENTIALS;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.LIST_SCALEIO_COMPONENTS;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.LIST_VCENTER_COMPONENTS;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.REBOOT_HOST;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.REMOTE_COMMAND_EXECUTION;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.UPDATE_SDC_PERFORMANCE_PROFILE;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.UPDATE_SOFTWARE_ACCEPTANCE;
import static com.dell.cpsd.paqx.dne.exception.TaskResponseExceptionCode.VM_POWER_OPERATIONS;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The tests for AmqpNodeService class.
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */
public class AmqpNodeServiceTest
{
    /**
     * Test that the listDiscoveredNodes method can hadle a timeout.
     *
     * @throws Exception
     */
    @Test(expected = ServiceTimeoutException.class)
    public void testListDiscoveredNodesTimeout() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("blah");
            }
        };

        nodeService.listDiscoveredNodes();
    }

    /**
     * Test that the listDiscoveredNodes method can execute successfully.
     *
     * @throws Exception
     */
    @Test
    public void testListDiscoveredNodesSuccess() throws Exception
    {
        String convergedUuid = UUID.randomUUID().toString();
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {

                com.dell.cpsd.MessageProperties messageProperties = new com.dell.cpsd.MessageProperties(Calendar.getInstance().getTime(),
                        UUID.randomUUID().toString(), "replyToMe");

                NodesListed listed = new NodesListed(messageProperties, buildNodeList(convergedUuid));
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, listed, null));
            }
        };

        List<DiscoveredNode> discovereds = nodeService.listDiscoveredNodes();
        Assert.assertEquals(1, discovereds.size());

        DiscoveredNode discovered = discovereds.get(0);
        Assert.assertEquals(convergedUuid, discovered.getConvergedUuid());

        Mockito.verify(dneProducer, Mockito.times(1)).publishListNodes(any());
    }

    private List<com.dell.cpsd.DiscoveredNode> buildNodeList(String uuid)
    {
        com.dell.cpsd.DiscoveredNode discoveredNode = new com.dell.cpsd.DiscoveredNode();
        discoveredNode.setConvergedUuid(uuid);
        discoveredNode.setAllocationStatus(com.dell.cpsd.DiscoveredNode.AllocationStatus.DISCOVERED);
        discoveredNode.setSerial("testserial");
        discoveredNode.setProduct("testproduct");
        discoveredNode.setVendor("test_Dell Inc");

        com.dell.cpsd.DiscoveredNode addedNode = new com.dell.cpsd.DiscoveredNode();
        addedNode.setConvergedUuid(UUID.randomUUID().toString());
        addedNode.setAllocationStatus(com.dell.cpsd.DiscoveredNode.AllocationStatus.ADDED);
        addedNode.setSerial("testserial");
        addedNode.setProduct("testproduct");
        addedNode.setVendor("test_Dell Inc");

        List<com.dell.cpsd.DiscoveredNode> nodeList = new ArrayList<>();
        nodeList.add(discoveredNode);
        nodeList.add(addedNode);

        return nodeList;
    }

    /**
     * Test that the listDiscoveredNodes method can handle any errors.
     *
     * @throws Exception
     */
    @Test(expected = ServiceExecutionException.class)
    public void testListDiscoveredNodesError() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);
        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "blah", "blah"));
            }
        };

        nodeService.listDiscoveredNodes();
    }

    /**
     * Test that the the listDiscoveredNodeInfo method
     * <p>
     * * @throws Exception
     */
    @Test
    public void testListDiscoveredNodeInfo() throws ServiceTimeoutException, ServiceExecutionException, JsonProcessingException
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);
        H2DataRepository repository = mock(H2DataRepository.class);

        String convergedUuid1 = UUID.randomUUID().toString();
        com.dell.cpsd.MessageProperties messageProperties = new com.dell.cpsd.MessageProperties(Calendar.getInstance().getTime(),
                UUID.randomUUID().toString(), "replyToMe");

        NodesListed listed = new NodesListed(messageProperties, buildNodeList(convergedUuid1));

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected ServiceResponse<?> processRequest(long timeout, ServiceRequestCallback serviceRequestCallback)
            {
                StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

                if (stackTraceElements[2].getMethodName().equalsIgnoreCase("listDiscoveredNodes"))
                {
                    ServiceResponse response = new ServiceResponse<>(serviceRequestCallback.getRequestId(), listed, "test");

                    return response;
                }

                return null;
            }
        };

        final List<DiscoveredNodeInfo> nodeInfos = nodeService.listDiscoveredNodeInfo();

        assertEquals(1, nodeInfos.size());
        assertEquals("testserial", nodeInfos.get(0).getSerialNumber());
        assertEquals("testproduct", nodeInfos.get(0).getProduct());
        Mockito.verify(repository).saveDiscoveredNodeInfo(any());
    }

    /**
     * /**
     * Test that the the listClusters method can handle a timeout.
     *
     * @throws Exception
     */
    @Test(expected = ServiceTimeoutException.class)
    public void testListClustersTimeout() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", mock(DataServiceRepository.class), null, null,
                null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("blah");
            }
        };

        nodeService.listClusters();
    }

    /**
     * Test that the listClusters method can execute successfully.
     *
     * @throws Exception
     */
    @Test
    public void testListClustersSuccess() throws Exception
    {
        String clusterName = UUID.randomUUID().toString();
        Integer numberOfHosts = 999;

        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", mock(DataServiceRepository.class), null, null,
                null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                List<String> nodeIds = new ArrayList<>();
                DiscoverClusterResponseInfo responseInfo = new DiscoverClusterResponseInfo(
                        Collections.singletonList(new ClusterInfo(clusterName, numberOfHosts)), nodeIds);

                MessageProperties messageProperties = new MessageProperties(Calendar.getInstance().getTime(), UUID.randomUUID().toString(),
                        "replyToMe");

                DiscoverClusterResponseInfoMessage responseInfoMessage = new DiscoverClusterResponseInfoMessage(messageProperties,
                        responseInfo, DiscoverClusterResponseInfoMessage.Status.SUCCESS, "");
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseInfoMessage, null));
            }
        };

        List<ClusterInfo> discovereds = nodeService.listClusters();
        Assert.assertEquals(1, discovereds.size());

        ClusterInfo discovered = discovereds.get(0);
        Assert.assertEquals(clusterName, discovered.getName());
        Assert.assertEquals(numberOfHosts, discovered.getNumberOfHosts());

        Mockito.verify(dneProducer, Mockito.times(1)).publishDiscoverClusters(any());
    }

    /**
     * Test that the listClusters method can handle any errors.
     *
     * @throws Exception
     */
    @Test(expected = ServiceExecutionException.class)
    public void testListClustersError() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);
        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "blah", "blah"));
            }
        };

        nodeService.listDiscoveredNodes();// ???
    }

    /**
     * Test that the idracNetworkSettings method can execute successfully.
     *
     * @throws ServiceTimeoutException
     * @throws ServiceExecutionException
     */
    @Test
    public void testIdracNetworkSettingsSuccess() throws ServiceTimeoutException, ServiceExecutionException
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);
        IdracNetworkSettingsRequest idracNetworkSettingsRequest = new IdracNetworkSettingsRequest("nodeId", "idracIpAddress",
                "idracGatewayIpAddress", "idracSubnetMask");

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                IdracNetworkSettingsResponse idracNetworkSettingsResponse = new IdracNetworkSettingsResponse("SUCCESS", "nodeId",
                        "idracIpAddress", "netmask", "gateway");
                com.dell.cpsd.rackhd.adapter.rabbitmq.MessageProperties messageProperties = new com.dell.cpsd.rackhd.adapter.rabbitmq.MessageProperties();
                messageProperties.setCorrelationId(UUID.randomUUID().toString());

                IdracNetworkSettingsResponseMessage responseMessage = new IdracNetworkSettingsResponseMessage(messageProperties,
                        idracNetworkSettingsResponse);

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        IdracInfo idracInfo = nodeService.idracNetworkSettings(idracNetworkSettingsRequest);

        Assert.assertNotNull(idracInfo);
        Assert.assertEquals("SUCCESS", idracInfo.getMessage());
        Assert.assertEquals("nodeId", idracInfo.getNodeId());

        Mockito.verify(dneProducer, Mockito.times(1)).publishIdracNetwokSettings(any());
    }

    /**
     * Test that the idracNetworkSettings method can handle any errors.
     *
     * @throws ServiceTimeoutException
     */
    @Test
    public void testIdracNetworkSettingsError() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);
        IdracNetworkSettingsRequest idracNetworkSettingsRequest = new IdracNetworkSettingsRequest("nodeId", "idracIpAddress",
                "idracGatewayIpAddress", "idracSubnetMask");
        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        nodeService.idracNetworkSettings(idracNetworkSettingsRequest);
        Mockito.verify(dneProducer, Mockito.times(1)).publishIdracNetwokSettings(any());
    }

    /**
     * Test that the notifyNodeAllocationStatus method executes successfully.
     *
     * @throws ServiceTimeoutException
     * @throws ServiceExecutionException
     */
    @Test
    public void testNotifyNodeAllocationCompleteSuccess() throws ServiceTimeoutException, ServiceExecutionException
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                com.dell.cpsd.MessageProperties messageProperties = new com.dell.cpsd.MessageProperties();
                messageProperties.setCorrelationId(UUID.randomUUID().toString());

                NodeAllocationInfo nodeAllocationInfo = new NodeAllocationInfo("elementIdentifier", "nodeIdentifier",
                        AllocationStatus.ADDED);

                CompleteNodeAllocationResponseMessage responseMessage = new CompleteNodeAllocationResponseMessage(messageProperties,
                        CompleteNodeAllocationResponseMessage.Status.SUCCESS, nodeAllocationInfo, Collections.emptyList());

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        Boolean responseInfo = nodeService.notifyNodeAllocationStatus("elementIdentifier", "Completed");

        Assert.assertEquals(true, responseInfo);
        Mockito.verify(dneProducer, Mockito.times(1)).publishCompleteNodeAllocation(any());
    }

    @Test
    public void testNotifyNodeAllocationStartedSuccess() throws ServiceTimeoutException, ServiceExecutionException
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                com.dell.cpsd.MessageProperties messageProperties = new com.dell.cpsd.MessageProperties();
                messageProperties.setCorrelationId(UUID.randomUUID().toString());

                NodeAllocationInfo nodeAllocationInfo = new NodeAllocationInfo("elementIdentifier", "nodeIdentifier",
                        AllocationStatus.PROVISIONING_IN_PROGRESS);

                StartNodeAllocationResponseMessage responseMessage = new StartNodeAllocationResponseMessage(messageProperties,
                        StartNodeAllocationResponseMessage.Status.SUCCESS, nodeAllocationInfo, Collections.emptyList());

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        Boolean responseInfo = nodeService.notifyNodeAllocationStatus("elementIdentifier", "Started");

        Assert.assertEquals(true, responseInfo);
        Mockito.verify(dneProducer, Mockito.times(1)).publishStartedNodeAllocation(any());
    }

    @Test
    public void testNotifyNodeAllocationStartedFailed() throws ServiceTimeoutException, ServiceExecutionException
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                com.dell.cpsd.MessageProperties messageProperties = new com.dell.cpsd.MessageProperties();
                messageProperties.setCorrelationId(UUID.randomUUID().toString());

                NodeAllocationInfo nodeAllocationInfo = new NodeAllocationInfo("elementIdentifier", "nodeIdentifier",
                        AllocationStatus.PROVISIONING_FAILED);

                FailNodeAllocationResponseMessage responseMessage = new FailNodeAllocationResponseMessage(messageProperties,
                        FailNodeAllocationResponseMessage.Status.SUCCESS, nodeAllocationInfo, Collections.emptyList());

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        Boolean responseInfo = nodeService.notifyNodeAllocationStatus("elementIdentifier", "failed");

        Assert.assertEquals(true, responseInfo);
        Mockito.verify(dneProducer, Mockito.times(1)).publishFailedNodeAllocation(any());
    }

    /**
     * Test that the notifyNodeAllocationStatus method can handle any errors.
     *
     * @throws ServiceTimeoutException
     */
    @Test(expected = ServiceExecutionException.class)
    public void testNotifyNodeAllocationCompleteError() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        nodeService.notifyNodeAllocationStatus("elementIdentifier", "Completed");
        Mockito.verify(dneProducer, Mockito.times(1)).publishCompleteNodeAllocation(any());
    }

    @Test(expected = ServiceExecutionException.class)
    public void testNotifyNodeAllocationStartedError() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        nodeService.notifyNodeAllocationStatus("elementIdentifier", "Started");
        Mockito.verify(dneProducer, Mockito.times(1)).publishStartedNodeAllocation(any());
    }

    @Test(expected = ServiceExecutionException.class)
    public void testNotifyNodeAllocationFailedError() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        nodeService.notifyNodeAllocationStatus("elementIdentifier", "failed");
        Mockito.verify(dneProducer, Mockito.times(1)).publishFailedNodeAllocation(any());
    }

    /**
     * Test that the changeIdracCredentialsComplete method executes successfully.
     *
     * @throws ServiceTimeoutException
     * @throws ServiceExecutionException
     */
    @Test
    public void testChangeIdracCredentialsCompleteSuccess() throws ServiceTimeoutException, ServiceExecutionException
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                com.dell.cpsd.MessageProperties messageProperties = new com.dell.cpsd.MessageProperties();
                messageProperties.setCorrelationId(UUID.randomUUID().toString());

                ChangeIdracCredentialsResponse changeCredentialsResponse = new ChangeIdracCredentialsResponse();
                changeCredentialsResponse.setNodeId("dummyNodeId");
                changeCredentialsResponse.setMessage("SUCCESS");

                ChangeIdracCredentialsResponseMessage responseMessage = new ChangeIdracCredentialsResponseMessage(messageProperties,
                        ChangeIdracCredentialsResponseMessage.Status.SUCCESS, Collections.emptyList());

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        ChangeIdracCredentialsResponse responseInfo = nodeService.changeIdracCredentials("dummyNodeId");

        Assert.assertEquals("SUCCESS", responseInfo.getMessage());
        Mockito.verify(dneProducer, Mockito.times(1)).publishChangeIdracCredentials(any());
    }

    /**
     * Test that the changeIdracCredentialsComplete method can handle any errors.
     *
     * @throws ServiceTimeoutException
     */
    @Test
    public void testChangeIdracCredentialsCompleteError() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        nodeService.changeIdracCredentials("dummyNodeId");
        Mockito.verify(dneProducer, Mockito.times(1)).publishChangeIdracCredentials(any());
    }

    @Test
    public void testListScaleIoComponentsFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestScaleIoComponents();
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == LIST_SCALEIO_COMPONENTS.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishListScaleIoComponents(any());
    }

    @Test
    public void testListScaleIoComponentsSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final com.dell.cpsd.storage.capabilities.api.MessageProperties messageProperties = new com.dell.cpsd.storage.capabilities.api.MessageProperties(
                new Date(), UUID.randomUUID().toString(), "test");
        final ListComponentResponseMessage responseMessage = mock(ListComponentResponseMessage.class);
        final ScaleIOComponentDetails scaleIOComponentDetails = mock(ScaleIOComponentDetails.class);
        final ScaleIoEndpointDetails scaleIoEndpointDetails = mock(ScaleIoEndpointDetails.class);
        final CredentialNameId credentialNameId = mock(CredentialNameId.class);

        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getComponents()).thenReturn(Collections.singletonList(scaleIOComponentDetails));
        when(scaleIOComponentDetails.getEndpoints()).thenReturn(Collections.singletonList(scaleIoEndpointDetails));
        when(scaleIoEndpointDetails.getCredentialNameIds()).thenReturn(Collections.singletonList(credentialNameId));

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                responseMessage.setMessageProperties(messageProperties);
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(repository.saveScaleIoComponentDetails(anyList())).thenReturn(true);

        nodeService.requestScaleIoComponents();

        Mockito.verify(dneProducer, Mockito.times(1)).publishListScaleIoComponents(any());
    }

    @Test
    public void testListVCenterComponentsFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestVCenterComponents();
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == LIST_VCENTER_COMPONENTS.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishListVCenterComponents(any());
    }

    @Test
    public void testListVCenterComponentsSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");
        final ListComponentsResponseMessage responseMessage = mock(ListComponentsResponseMessage.class);
        final VCenterComponentDetails vCenterComponentDetails = mock(VCenterComponentDetails.class);
        final VCenterEndpointDetails vCenterEndpointDetails = mock(VCenterEndpointDetails.class);
        final VCenterCredentialDetails credentialDetail = mock(VCenterCredentialDetails.class);

        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getVcenterComponentDetails()).thenReturn(Collections.singletonList(vCenterComponentDetails));
        when(vCenterComponentDetails.getEndpoints()).thenReturn(Collections.singletonList(vCenterEndpointDetails));
        when(vCenterEndpointDetails.getCredentialDetails()).thenReturn(Collections.singletonList(credentialDetail));

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                responseMessage.setMessageProperties(messageProperties);
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(repository.saveVCenterComponentDetails(anyList())).thenReturn(true);

        nodeService.requestVCenterComponents();

        Mockito.verify(dneProducer, Mockito.times(1)).publishListVCenterComponents(any());
    }

    @Test
    public void testDiscoverVCenterFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DiscoveryInfoToVCenterDomainTransformer transformer = mock(DiscoveryInfoToVCenterDomainTransformer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds componentEndpointIds = mock(
                com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, transformer, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestDiscoverVCenter(componentEndpointIds, "job-id");
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == DISCOVER_VCENTER.getCode());
        }

        Mockito.verify(dneProducer, Mockito.times(1)).publishDiscoverVcenter(any());
    }

    @Test
    public void testDiscoverVCenterSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DiscoveryInfoToVCenterDomainTransformer transformer = mock(DiscoveryInfoToVCenterDomainTransformer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds componentEndpointIds = mock(
                com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, transformer, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

                final DiscoveryResponseInfoMessage responseMessage = new DiscoveryResponseInfoMessage(messageProperties,
                        Collections.singletonList(new Datacenter()), "");

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(transformer.transform(any())).thenReturn(new VCenter());
        when(repository.saveVCenterData(anyString(), any())).thenReturn(true);

        nodeService.requestDiscoverVCenter(componentEndpointIds, "job-id");

        Mockito.verify(dneProducer, Mockito.times(1)).publishDiscoverVcenter(any());
    }

    @Test
    public void testDiscoverScaleIoFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final ScaleIORestToScaleIODomainTransformer transformer = mock(ScaleIORestToScaleIODomainTransformer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds componentEndpointIds = mock(
                com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, transformer, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestDiscoverScaleIo(componentEndpointIds, "job-id");
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == DISCOVER_SCALEIO.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishDiscoverScaleIo(any());
    }

    @Test
    public void testDiscoverScaleIoSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final ScaleIORestToScaleIODomainTransformer transformer = mock(ScaleIORestToScaleIODomainTransformer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds componentEndpointIds = mock(
                com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, transformer, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                final com.dell.cpsd.storage.capabilities.api.MessageProperties messageProperties = new com.dell.cpsd.storage.capabilities.api.MessageProperties(
                        new Date(), UUID.randomUUID().toString(), "test");

                final ListStorageResponseMessage responseMessage = new ListStorageResponseMessage(messageProperties,
                        new ScaleIOSystemDataRestRep(), "");

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(transformer.transform(any())).thenReturn(new ScaleIOData());
        when(repository.saveScaleIoData(anyString(), any())).thenReturn(true);

        nodeService.requestDiscoverScaleIo(componentEndpointIds, "job-id");

        Mockito.verify(dneProducer, Mockito.times(1)).publishDiscoverScaleIo(any());
    }

    @Test
    public void testApplyEsxiLicenseFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final AddEsxiHostVSphereLicenseRequest request = mock(AddEsxiHostVSphereLicenseRequest.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestInstallEsxiLicense(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == APPLY_ESXI_HOST_LICENSE.getCode());
        }

        Mockito.verify(dneProducer, Mockito.times(1)).publishApplyEsxiLicense(request);
    }

    @Test
    public void testApplyEsxiLicenseSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final AddEsxiHostVSphereLicenseRequest request = mock(AddEsxiHostVSphereLicenseRequest.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

                final AddEsxiHostVSphereLicenseResponse responseMessage = new AddEsxiHostVSphereLicenseResponse(messageProperties,
                        AddEsxiHostVSphereLicenseResponse.Status.SUCCESS, "");

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        nodeService.requestInstallEsxiLicense(request);

        Mockito.verify(dneProducer, Mockito.times(1)).publishApplyEsxiLicense(request);
    }

    @Test
    public void testHostMaintenanceFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final HostMaintenanceModeRequestMessage request = mock(HostMaintenanceModeRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestHostMaintenanceMode(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == HOST_MAINTENANCE_MODE.getCode());
        }

        Mockito.verify(dneProducer, Mockito.times(1)).publishHostMaintenanceMode(request);
    }

    @Test
    public void testHostMaintenanceSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final HostMaintenanceModeRequestMessage request = mock(HostMaintenanceModeRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

                final HostMaintenanceModeResponseMessage responseMessage = new HostMaintenanceModeResponseMessage(messageProperties,
                        HostMaintenanceModeResponseMessage.Status.SUCCESS, "");

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        nodeService.requestHostMaintenanceMode(request);

        Mockito.verify(dneProducer, Mockito.times(1)).publishHostMaintenanceMode(request);
    }

    @Test
    public void testAddHostToVCenterFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final ClusterOperationRequestMessage request = mock(ClusterOperationRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestAddHostToVCenter(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == ADD_HOST_TO_VCENTER_CLUSTER.getCode());
        }

        Mockito.verify(dneProducer, Mockito.times(1)).publishAddHostToVCenter(request);
    }

    @Test
    public void testAddHostToVCenterSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final ClusterOperationRequestMessage request = mock(ClusterOperationRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

                final ClusterOperationResponseMessage responseMessage = new ClusterOperationResponseMessage(messageProperties,
                        ClusterOperationResponseMessage.Status.SUCCESS, "");

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        nodeService.requestAddHostToVCenter(request);

        Mockito.verify(dneProducer, Mockito.times(1)).publishAddHostToVCenter(request);
    }

    @Test
    public void testAddHostToDvSwitchFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final AddHostToDvSwitchRequestMessage request = mock(AddHostToDvSwitchRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestAddHostToDvSwitch(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == ADD_HOST_TO_DV_SWITCH.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishAddHostToDvSwitch(request);
    }

    @Test
    public void testAddHostToDvSwitchSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final AddHostToDvSwitchRequestMessage request = mock(AddHostToDvSwitchRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

                final AddHostToDvSwitchResponseMessage responseMessage = new AddHostToDvSwitchResponseMessage(messageProperties,
                        AddHostToDvSwitchResponseMessage.Status.SUCCESS, "");

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        try
        {
            nodeService.requestAddHostToDvSwitch(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == ADD_HOST_TO_DV_SWITCH.getCode());
        }

        Mockito.verify(dneProducer, Mockito.times(1)).publishAddHostToDvSwitch(request);
    }

    @Test
    public void testDeployScaleIoVmFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final DeployVMFromTemplateRequestMessage request = mock(DeployVMFromTemplateRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestDeployScaleIoVm(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == DEPLOY_VM_FROM_TEMPLATE.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishDeployVmFromTemplate(request);
    }

    @Test
    public void testDeployScaleIoVmSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final DeployVMFromTemplateRequestMessage request = mock(DeployVMFromTemplateRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

                final DeployVMFromTemplateResponseMessage responseMessage = new DeployVMFromTemplateResponseMessage(messageProperties,
                        DeployVMFromTemplateResponseMessage.Status.SUCCESS, "");

                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        nodeService.requestDeployScaleIoVm(request);

        Mockito.verify(dneProducer, Mockito.times(1)).publishDeployVmFromTemplate(request);
    }

    @Test
    public void testEnablePciPassThroughFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final EnablePCIPassthroughRequestMessage request = mock(EnablePCIPassthroughRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestEnablePciPassThrough(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == ENABLE_PCI_PASSTHROUGH.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishEnablePciPassthrough(request);
    }

    @Test
    public void testEnablePciPassThroughSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final EnablePCIPassthroughRequestMessage request = mock(EnablePCIPassthroughRequestMessage.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

        final EnablePCIPassthroughResponseMessage responseMessage = mock(EnablePCIPassthroughResponseMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(EnablePCIPassthroughResponseMessage.Status.SUCCESS_REBOOT_REQUIRED);

        nodeService.requestEnablePciPassThrough(request);

        Mockito.verify(dneProducer, Mockito.times(1)).publishEnablePciPassthrough(request);
    }

    @Test
    public void testRebootHostFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final HostPowerOperationRequestMessage request = mock(HostPowerOperationRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestHostReboot(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == REBOOT_HOST.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishRebootHost(request);
    }

    @Test
    public void testRebootHostSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final HostPowerOperationRequestMessage request = mock(HostPowerOperationRequestMessage.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

        final HostPowerOperationResponseMessage responseMessage = mock(HostPowerOperationResponseMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(HostPowerOperationResponseMessage.Status.SUCCESS);

        nodeService.requestHostReboot(request);

        Mockito.verify(dneProducer, Mockito.times(1)).publishRebootHost(request);
    }

    @Test
    public void testConfigurePciPassThroughFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final UpdatePCIPassthruSVMRequestMessage request = mock(UpdatePCIPassthruSVMRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestSetPciPassThrough(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == CONFIGURE_PCI_PASSTHROUGH_SCALEIO_VM.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishSetPciPassthrough(request);
    }

    @Test
    public void testConfigurePciPassThroughSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final UpdatePCIPassthruSVMRequestMessage request = mock(UpdatePCIPassthruSVMRequestMessage.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

        final UpdatePCIPassthruSVMResponseMessage responseMessage = mock(UpdatePCIPassthruSVMResponseMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(UpdatePCIPassthruSVMResponseMessage.Status.SUCCESS);

        try
        {
            nodeService.requestSetPciPassThrough(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == CONFIGURE_PCI_PASSTHROUGH_SCALEIO_VM.getCode());
        }

        Mockito.verify(dneProducer, Mockito.times(1)).publishSetPciPassthrough(request);
    }

    @Test
    public void testConfigureSoftwareVibFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final SoftwareVIBConfigureRequestMessage request = mock(SoftwareVIBConfigureRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestConfigureScaleIoVib(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == CONFIGURE_SDC_VIB.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishConfigureScaleIoVib(request);
    }

    @Test
    public void testConfigureSoftwareVibSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final SoftwareVIBConfigureRequestMessage request = mock(SoftwareVIBConfigureRequestMessage.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

        final SoftwareVIBResponseMessage responseMessage = mock(SoftwareVIBResponseMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(SoftwareVIBResponseMessage.Status.SUCCESS);

        nodeService.requestConfigureScaleIoVib(request);

        Mockito.verify(dneProducer, Mockito.times(1)).publishConfigureScaleIoVib(request);
    }

    @Test
    public void testInstallSoftwareVibFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final SoftwareVIBRequestMessage request = mock(SoftwareVIBRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.requestInstallSoftwareVib(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == INSTALL_SDC_VIB.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishInstallScaleIoVib(request);
    }

    @Test
    public void testInstallSoftwareVibSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final SoftwareVIBRequestMessage request = mock(SoftwareVIBRequestMessage.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

        final SoftwareVIBResponseMessage responseMessage = mock(SoftwareVIBResponseMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(SoftwareVIBResponseMessage.Status.SUCCESS);

        nodeService.requestInstallSoftwareVib(request);

        Mockito.verify(dneProducer, Mockito.times(1)).publishInstallScaleIoVib(request);
    }

    @Test
    public void testListEsxiDefaultCredentialDetailsFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final ListEsxiCredentialDetailsRequestMessage request = mock(ListEsxiCredentialDetailsRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        try
        {
            nodeService.listDefaultCredentials(request);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == LIST_ESXI_DEFAULT_CREDENTIALS.getCode());
        }
        Mockito.verify(dneProducer, Mockito.times(1)).publishListExsiCredentialDetails(request);
    }

    @Test
    public void testListEsxiDefaultCredentialDetailsSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final ListEsxiCredentialDetailsRequestMessage request = mock(ListEsxiCredentialDetailsRequestMessage.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

        final ListEsxiCredentialDetailsResponseMessage responseMessage = mock(ListEsxiCredentialDetailsResponseMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getComponentUuid()).thenReturn("comp-uuid");
        when(responseMessage.getEndpointUuid()).thenReturn("end-uuid");
        when(responseMessage.getCredentialUuid()).thenReturn("cred-uuid");

        final ComponentEndpointIds componentEndpointIds = nodeService.listDefaultCredentials(request);

        assertEquals(componentEndpointIds.getComponentUuid(), "comp-uuid");
        assertEquals(componentEndpointIds.getEndpointUuid(), "end-uuid");
        assertEquals(componentEndpointIds.getCredentialUuid(), "cred-uuid");

        Mockito.verify(dneProducer, Mockito.times(1)).publishListExsiCredentialDetails(request);
    }

    /**
     * Test that the listNodeInventory method can handle a timeout.
     *
     * @throws Exception
     */
    @Test(expected = ServiceTimeoutException.class)
    public void testListNodeInventory() throws Exception
    {
        DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        DneProducer dneProducer = mock(DneProducer.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("TIMEOUT_TEST");
            }
        };

        nodeService.listNodeInventory("FAKE_UUID");
    }

    @Test
    public void testRequestDatastoreRenameSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");
        final DatastoreRenameResponseMessage responseMessage = mock(DatastoreRenameResponseMessage.class);
        final DatastoreRenameRequestMessage requestMessage = mock(DatastoreRenameRequestMessage.class);
        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(DatastoreRenameResponseMessage.Status.SUCCESS);
        when(responseMessage.getDatastoreName()).thenReturn("DAS100");

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        final String datastoreName = nodeService.requestDatastoreRename(requestMessage);

        assertNotNull(datastoreName);
        Mockito.verify(dneProducer).publishDatastoreRename(any(DatastoreRenameRequestMessage.class));
    }

    @Test
    public void testRequestDatastoreRenameException() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DatastoreRenameRequestMessage requestMessage = mock(DatastoreRenameRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("TIMEOUT_TEST");
            }
        };

        try
        {
            nodeService.requestDatastoreRename(requestMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == DATASTORE_RENAME.getCode());
            assertEquals("TIMEOUT_TEST", ex.getMessage());
        }

        Mockito.verify(dneProducer).publishDatastoreRename(any(DatastoreRenameRequestMessage.class));
    }

    @Test
    public void testRequestUpdateSoftwareAcceptanceSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");
        final VCenterUpdateSoftwareAcceptanceResponseMessage responseMessage = mock(VCenterUpdateSoftwareAcceptanceResponseMessage.class);
        final VCenterUpdateSoftwareAcceptanceRequestMessage requestMessage = mock(VCenterUpdateSoftwareAcceptanceRequestMessage.class);
        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(VCenterUpdateSoftwareAcceptanceResponseMessage.Status.SUCCESS);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        nodeService.requestUpdateSoftwareAcceptance(requestMessage);

        Mockito.verify(dneProducer).publishUpdateSoftwareAcceptance(any(VCenterUpdateSoftwareAcceptanceRequestMessage.class));
    }

    @Test
    public void testRequestUpdateSoftwareAcceptanceException() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final VCenterUpdateSoftwareAcceptanceRequestMessage requestMessage = mock(VCenterUpdateSoftwareAcceptanceRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("TIMEOUT_TEST");
            }
        };

        try
        {
            nodeService.requestUpdateSoftwareAcceptance(requestMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == UPDATE_SOFTWARE_ACCEPTANCE.getCode());
            assertEquals("TIMEOUT_TEST", ex.getMessage());
        }

        Mockito.verify(dneProducer).publishUpdateSoftwareAcceptance(any(VCenterUpdateSoftwareAcceptanceRequestMessage.class));
    }

    @Test
    public void testRequestVmPowerOperationSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");
        final VmPowerOperationsResponseMessage responseMessage = mock(VmPowerOperationsResponseMessage.class);
        final VmPowerOperationsRequestMessage requestMessage = mock(VmPowerOperationsRequestMessage.class);
        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(VmPowerOperationsResponseMessage.Status.SUCCESS);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        nodeService.requestVmPowerOperation(requestMessage);

        Mockito.verify(dneProducer).publishVmPowerOperation(any(VmPowerOperationsRequestMessage.class));
    }

    @Test
    public void testRequestVmPowerOperationException() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final VmPowerOperationsRequestMessage requestMessage = mock(VmPowerOperationsRequestMessage.class);
        final String errorMessage = "TIMEOUT_TEST";

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException(errorMessage);
            }
        };

        try
        {
            nodeService.requestVmPowerOperation(requestMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == VM_POWER_OPERATIONS.getCode());
            assertEquals(errorMessage, ex.getMessage());
        }

        Mockito.verify(dneProducer).publishVmPowerOperation(any(VmPowerOperationsRequestMessage.class));
    }

    @Test(expected = ServiceTimeoutException.class)
    public void testvalidateProtectionDomains() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final EssValidateProtectionDomainsRequestMessage requestMessage = mock(EssValidateProtectionDomainsRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("TIMEOUT_TEST");
            }
        };
        nodeService.validateProtectionDomains(requestMessage);
        Mockito.verify(dneProducer).publishValidateProtectionDomain(any(EssValidateProtectionDomainsRequestMessage.class));
    }

    @Test
    public void testvalidateProtectionDomainsSuccess() throws ServiceExecutionException, ServiceTimeoutException
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final EssValidateProtectionDomainsRequestMessage request = mock(EssValidateProtectionDomainsRequestMessage.class);
        final EssValidateProtectionDomainsResponseMessage responseMessage = mock(EssValidateProtectionDomainsResponseMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };
        EssValidateProtectionDomainsResponseMessage response = nodeService.validateProtectionDomains(request);
        Assert.assertNotNull(response.getValidProtectionDomains());
        Assert.assertNull(response.getError());
    }

    @Test(expected = ServiceExecutionException.class)
    public void testvalidateProtectionDomainsFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final EssValidateProtectionDomainsRequestMessage requestMessage = mock(EssValidateProtectionDomainsRequestMessage.class);
        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        nodeService.validateProtectionDomains(requestMessage);
        Mockito.verify(dneProducer, Mockito.times(1)).publishValidateProtectionDomain(any());
    }

    @Test
    public void testRequestConfigureVmNetworkSettingsSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");
        final ConfigureVmNetworkSettingsResponseMessage responseMessage = mock(ConfigureVmNetworkSettingsResponseMessage.class);
        final ConfigureVmNetworkSettingsRequestMessage requestMessage = mock(ConfigureVmNetworkSettingsRequestMessage.class);
        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(ConfigureVmNetworkSettingsResponseMessage.Status.SUCCESS);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        try
        {
            nodeService.requestConfigureVmNetworkSettings(requestMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == CONFIGURE_VM_NETWORK_SETTINGS.getCode());
        }

        Mockito.verify(dneProducer).publishConfigureVmNetworkSettings(any(ConfigureVmNetworkSettingsRequestMessage.class));
    }

    @Test
    public void testRequestConfigureVmNetworkSettingsException() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final ConfigureVmNetworkSettingsRequestMessage requestMessage = mock(ConfigureVmNetworkSettingsRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("TIMEOUT_TEST");
            }
        };

        try
        {
            nodeService.requestConfigureVmNetworkSettings(requestMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == CONFIGURE_VM_NETWORK_SETTINGS.getCode());
            assertEquals("TIMEOUT_TEST", ex.getMessage());
        }

        Mockito.verify(dneProducer).publishConfigureVmNetworkSettings(any(ConfigureVmNetworkSettingsRequestMessage.class));
    }

    @Test
    public void testRequestRemoteCommandExecutionSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");
        final RemoteCommandExecutionResponseMessage responseMessage = mock(RemoteCommandExecutionResponseMessage.class);
        final RemoteCommandExecutionRequestMessage requestMessage = mock(RemoteCommandExecutionRequestMessage.class);
        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(RemoteCommandExecutionResponseMessage.Status.SUCCESS);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        nodeService.requestRemoteCommandExecution(requestMessage);

        Mockito.verify(dneProducer).publishRemoteCommandExecution(any(RemoteCommandExecutionRequestMessage.class));
    }

    @Test
    public void testRequestRemoteCommandExecutionException() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final RemoteCommandExecutionRequestMessage requestMessage = mock(RemoteCommandExecutionRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("TIMEOUT_TEST");
            }
        };

        try
        {
            nodeService.requestRemoteCommandExecution(requestMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == REMOTE_COMMAND_EXECUTION.getCode());
            assertEquals("TIMEOUT_TEST", ex.getMessage());
        }

        Mockito.verify(dneProducer).publishRemoteCommandExecution(any(RemoteCommandExecutionRequestMessage.class));
    }

    @Test
    public void testValidateStoragePoolsSuccess() throws Exception
    {
        final MessageProperties messageProperties = new MessageProperties(new Date(), UUID.randomUUID().toString(), "test");

        final List<ScaleIOStoragePool> scaleIOStoragePools = new ArrayList<ScaleIOStoragePool>();
        final List<com.dell.cpsd.service.engineering.standards.Device> newDevices = new ArrayList<com.dell.cpsd.service.engineering.standards.Device>();
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final EssValidateStoragePoolResponseMessage responseMessage = mock(EssValidateStoragePoolResponseMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null,
                new StoragePoolEssRequestTransformer())
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };
        EssValidateStoragePoolResponseMessage response = nodeService.validateStoragePools(scaleIOStoragePools, newDevices, anyMap(), null);
        Assert.assertNotNull(response);
    }

    @Test(expected = ServiceTimeoutException.class)
    public void testValidateStoragepools() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final List<ScaleIOStoragePool> scaleIOStoragePools = new ArrayList<>();
        final List<com.dell.cpsd.service.engineering.standards.Device> newDevices = new ArrayList<com.dell.cpsd.service.engineering.standards.Device>();

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null,
                new StoragePoolEssRequestTransformer())
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("TIMEOUT_TEST");
            }
        };
        nodeService.validateStoragePools(scaleIOStoragePools, newDevices, anyMap(), null);
        Mockito.verify(dneProducer).publishValidateStorage(any(EssValidateStoragePoolRequestMessage.class));
    }

    @Test(expected = ServiceExecutionException.class)
    public void testValidateStoragePoolFailure() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final List<ScaleIOStoragePool> scaleIOStoragePools = new ArrayList<ScaleIOStoragePool>();
        final List<com.dell.cpsd.service.engineering.standards.Device> newDevices = new ArrayList<com.dell.cpsd.service.engineering.standards.Device>();

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null,
                new StoragePoolEssRequestTransformer())
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceError(new ServiceError(requestId, "network", "network"));
            }
        };

        nodeService.validateStoragePools(scaleIOStoragePools, newDevices, anyMap(), null);
        Mockito.verify(dneProducer, Mockito.times(1)).publishValidateStorage(any());
    }

    @Test
    public void testRequestAddHostToProtectionDomainSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final com.dell.cpsd.storage.capabilities.api.MessageProperties messageProperties = new com.dell.cpsd.storage.capabilities.api.MessageProperties(
                new Date(), UUID.randomUUID().toString(), "test");
        final AddHostToProtectionDomainRequestMessage requestMessage = mock(AddHostToProtectionDomainRequestMessage.class);
        final AddHostToProtectionDomainResponseMessage responseMessage = mock(AddHostToProtectionDomainResponseMessage.class);
        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(AddHostToProtectionDomainResponseMessage.Status.SUCCESS);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        try
        {
            nodeService.requestAddHostToProtectionDomain(requestMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == ADD_SDS_NODE_TO_PROTECTION_DOMAIN.getCode());
        }

        Mockito.verify(dneProducer).publishAddHostToProtectionDomain(any(AddHostToProtectionDomainRequestMessage.class));
    }

    @Test
    public void testRequestAddHostToProtectionDomainException() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final AddHostToProtectionDomainRequestMessage requestMessage = mock(AddHostToProtectionDomainRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("TIMEOUT_TEST");
            }
        };

        try
        {
            nodeService.requestAddHostToProtectionDomain(requestMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == ADD_SDS_NODE_TO_PROTECTION_DOMAIN.getCode());
        }

        Mockito.verify(dneProducer).publishAddHostToProtectionDomain(any(AddHostToProtectionDomainRequestMessage.class));
    }

    @Test
    public void testRequestUpdateSdcPerformanceProfileSuccess() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final DataServiceRepository repository = mock(DataServiceRepository.class);
        final com.dell.cpsd.storage.capabilities.api.MessageProperties messageProperties = new com.dell.cpsd.storage.capabilities.api.MessageProperties(
                new Date(), UUID.randomUUID().toString(), "test");
        final SioSdcUpdatePerformanceProfileResponseMessage responseMessage = mock(SioSdcUpdatePerformanceProfileResponseMessage.class);
        final SioSdcUpdatePerformanceProfileRequestMessage requestMessage = mock(SioSdcUpdatePerformanceProfileRequestMessage.class);
        when(responseMessage.getMessageProperties()).thenReturn(messageProperties);
        when(responseMessage.getStatus()).thenReturn(SioSdcUpdatePerformanceProfileResponseMessage.Status.SUCCESS);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", repository, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                serviceCallback.handleServiceResponse(new ServiceResponse<>(requestId, responseMessage, null));
            }
        };

        nodeService.requestUpdateSdcPerformanceProfile(requestMessage);

        Mockito.verify(dneProducer).publishUpdateSdcPerformanceProfile(any(SioSdcUpdatePerformanceProfileRequestMessage.class));
    }

    @Test
    public void testRequestUpdateSdcPerformanceProfileException() throws Exception
    {
        final DelegatingMessageConsumer consumer = new DefaultMessageConsumer();
        final DneProducer dneProducer = mock(DneProducer.class);
        final SioSdcUpdatePerformanceProfileRequestMessage requestMessage = mock(SioSdcUpdatePerformanceProfileRequestMessage.class);

        AmqpNodeService nodeService = new AmqpNodeService(consumer, dneProducer, "replyToMe", null, null, null, null)
        {
            @Override
            protected void waitForServiceCallback(ServiceCallback serviceCallback, String requestId, long timeout)
                    throws ServiceTimeoutException
            {
                throw new ServiceTimeoutException("TIMEOUT_TEST");
            }
        };

        try
        {
            nodeService.requestUpdateSdcPerformanceProfile(requestMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            assertTrue(ex.getCode() == UPDATE_SDC_PERFORMANCE_PROFILE.getCode());
            assertEquals("TIMEOUT_TEST", ex.getMessage());
        }

        Mockito.verify(dneProducer).publishUpdateSdcPerformanceProfile(any(SioSdcUpdatePerformanceProfileRequestMessage.class));
    }
}
