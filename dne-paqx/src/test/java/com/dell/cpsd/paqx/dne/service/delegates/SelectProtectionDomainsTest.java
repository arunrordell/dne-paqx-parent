/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOData;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOProtectionDomain;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOSDS;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.service.engineering.standards.EssValidateProtectionDomainsResponseMessage;
import com.dell.cpsd.service.engineering.standards.NodeData;
import com.dell.cpsd.service.engineering.standards.ProtectionDomain;
import com.dell.cpsd.service.engineering.standards.ScaleIODataServer;
import com.dell.cpsd.service.engineering.standards.ValidProtectionDomain;
import com.dell.cpsd.virtualization.capabilities.api.MessageProperties;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAILS;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
public class SelectProtectionDomainsTest
{
    private SelectProtectionDomains                     selectedProtectionDomains;
    private NodeDetail                                  nodeDetail;
    private EssValidateProtectionDomainsResponseMessage responseMessage;
    private List<NodeDetail> nodeDetails = new ArrayList<>();

    @Mock
    private NodeService nodeService;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private MessageProperties messageProperties;

    @Mock
    private DataServiceRepository repository;

    @Before
    public void setUp() throws Exception
    {
        selectedProtectionDomains = new SelectProtectionDomains(nodeService, repository);
        NodeDetail nodeDetail = new NodeDetail("1", "abc");
        NodeDetail nodeDetail1 = new NodeDetail("2","abcd");

        nodeDetails.add(nodeDetail);
        nodeDetails.add(nodeDetail1);
        doReturn(nodeDetails).when(delegateExecution).getVariable(NODE_DETAILS);


        List<ScaleIOData> scaleIODataList = new ArrayList<>();
        ScaleIOData scaleIOData = new ScaleIOData("sio1", "name1", "installId", "mdmMode", "systemVersion", "clusterState", "version");

        ScaleIOProtectionDomain protectionDomain1 = new ScaleIOProtectionDomain("pdId1", "pdName1", "ACTIVE");

        ProtectionDomain protectionDomain = new ProtectionDomain();
        protectionDomain.setId(protectionDomain1.getId());
        protectionDomain.setName(protectionDomain1.getName());
        protectionDomain.setState("ACTIVE");

        ScaleIOSDS sds = new ScaleIOSDS("sds1", "sdsName-ESX", "RUNNING", 1234);
        sds.setProtectionDomain(protectionDomain1);
        protectionDomain1.addSDS(sds);
        scaleIOData.addProtectionDomain(protectionDomain1);
        protectionDomain1.setScaleIOData(scaleIOData);
        scaleIODataList.add(scaleIOData);
        doReturn(scaleIODataList).when(nodeService).listScaleIOData();

        NodeData nodeData= new NodeData();
        nodeData.setSymphonyUuid("1");
        NodeData nodeData1= new NodeData();
        nodeData1.setSymphonyUuid("2");

        ScaleIODataServer scaleIODataServer = new ScaleIODataServer();
        scaleIODataServer.setNodeData(nodeData);

        ScaleIODataServer scaleIODataServer1 = new ScaleIODataServer();
        scaleIODataServer1.setNodeData(nodeData1);

        List<ScaleIODataServer> scaleIODataServerList = new ArrayList<>();
        scaleIODataServerList.add(scaleIODataServer);
        scaleIODataServerList.add(scaleIODataServer1);

        protectionDomain.setScaleIODataServers(scaleIODataServerList);

        responseMessage = new EssValidateProtectionDomainsResponseMessage();
        ValidProtectionDomain vpd1 = new ValidProtectionDomain("pdId1", protectionDomain, null,null);
        responseMessage.setValidProtectionDomains(Arrays.asList(vpd1));
        doReturn(responseMessage).when(nodeService).validateProtectionDomains(any());
    }

    @Test
    public void testSuccessful()
    {
        selectedProtectionDomains.delegateExecute(delegateExecution);
        assertEquals(nodeDetails.get(0).getProtectionDomainId(),"pdId1");
        assertEquals(nodeDetails.get(1).getProtectionDomainId(), "pdId1");
    }
}
