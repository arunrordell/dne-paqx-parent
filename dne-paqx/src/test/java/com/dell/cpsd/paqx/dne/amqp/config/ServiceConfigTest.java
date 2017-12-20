/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.amqp.config;

import com.dell.cpsd.paqx.dne.amqp.producer.DneProducer;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.repository.JobRepository;
import com.dell.cpsd.paqx.dne.service.model.Step;
import com.dell.cpsd.paqx.dne.transformers.DiscoveryInfoToVCenterDomainTransformer;
import com.dell.cpsd.paqx.dne.transformers.HostToInstallEsxiRequestTransformer;
import com.dell.cpsd.paqx.dne.transformers.ScaleIORestToScaleIODomainTransformer;
import com.dell.cpsd.paqx.dne.transformers.StoragePoolEssRequestTransformer;
import com.dell.cpsd.service.common.client.rpc.DelegatingMessageConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * Test for {@link ServiceConfig}
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceConfigTest
{
    @Mock
    private DiscoveryInfoToVCenterDomainTransformer discoveryInfoToVCenterDomainTransformer;

    @Mock
    private ScaleIORestToScaleIODomainTransformer scaleIORestToScaleIODomainTransformer;

    @Mock
    private HostToInstallEsxiRequestTransformer hostToInstallEsxiRequestTransformer;

    @Mock
    private StoragePoolEssRequestTransformer storagePoolEssRequestTransformer;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private DataServiceRepository repository;

    @InjectMocks
    private ServiceConfig serviceConfig;

    @Mock
    private DelegatingMessageConsumer delegatingMessageConsumer;

    @Mock
    private DneProducer dneProducer;

    private String replyTo = "test";

    @Mock
    @Qualifier("addNodeWorkflowSteps")
    private Map<String, Step> addWorkflowSteps;

    @Mock
    @Qualifier("preProcessWorkflowSteps")
    private Map<String, Step> preWorkflowSteps;

    @Before
    public void setUp() throws Exception
    {
        ReflectionTestUtils.setField(serviceConfig, "threadCount", 10);
    }

    @Test
    public void testNodeServiceClient() throws Exception
    {
        assertNotNull(serviceConfig.nodeServiceClient(delegatingMessageConsumer, dneProducer, replyTo));
    }

    @Test
    public void testAddNodeWorkFlowService() throws Exception
    {
        assertNotNull(serviceConfig.addNodeWorkflowService(addWorkflowSteps));
    }

    @Test
    public void testPreprocessWorkflowService() throws Exception
    {
        assertNotNull(serviceConfig.preProcessWorkflowService(preWorkflowSteps));
    }

    @Test
    public void testDneTaskExecutorService() throws Exception
    {
        assertNotNull(serviceConfig.dneTaskExecutorService());
    }

    @Test
    public void testAddNodesToSystemDefinitionTaskExecutorService() throws Exception
    {
        assertNotNull(serviceConfig.addNodesToSystemDefinitionTaskExecutorService());
    }
}
