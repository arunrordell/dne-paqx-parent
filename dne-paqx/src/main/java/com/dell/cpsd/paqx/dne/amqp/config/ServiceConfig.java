/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.amqp.config;

import com.dell.cpsd.paqx.dne.amqp.producer.DneProducer;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.repository.JobRepository;
import com.dell.cpsd.paqx.dne.service.IpAddressValidator;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.WorkflowService;
import com.dell.cpsd.paqx.dne.service.WorkflowServiceImpl;
import com.dell.cpsd.paqx.dne.service.amqp.AmqpNodeService;
import com.dell.cpsd.paqx.dne.service.model.Step;
import com.dell.cpsd.paqx.dne.transformers.DiscoveryInfoToVCenterDomainTransformer;
import com.dell.cpsd.paqx.dne.transformers.HostToInstallEsxiRequestTransformer;
import com.dell.cpsd.paqx.dne.transformers.ScaleIORestToScaleIODomainTransformer;
import com.dell.cpsd.paqx.dne.transformers.StoragePoolEssRequestTransformer;
import com.dell.cpsd.sdk.AMQPClient;
import com.dell.cpsd.sdk.SystemDefinitionMessenger;
import com.dell.cpsd.sdk.config.SDKConfiguration;
import com.dell.cpsd.service.common.client.rpc.DelegatingMessageConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */

@Configuration
@ComponentScan({"com.dell.cpsd.paqx.dne.service", "com.dell.cpsd.paqx.dne.transformers", "com.dell.cpsd.paqx.dne.repository"})
@Import({RabbitConfig.class, ConsumerConfig.class, ProducerConfig.class, SDKConfiguration.class, SystemDefinitionMessenger.class,
        AMQPClient.class, PersistenceConfig.class})
public class ServiceConfig
{
    @Autowired
    private DiscoveryInfoToVCenterDomainTransformer discoveryInfoToVCenterDomainTransformer;

    @Autowired
    private ScaleIORestToScaleIODomainTransformer scaleIORestToScaleIODomainTransformer;

    @Autowired
    private HostToInstallEsxiRequestTransformer hostToInstallEsxiRequestTransformer;

    @Autowired
    private StoragePoolEssRequestTransformer storagePoolEssRequestTransformer;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DataServiceRepository repository;

    @Value("${executors.thread.count}")
    private Integer threadCount;

    @Bean
    public NodeService nodeServiceClient(@Autowired DelegatingMessageConsumer delegatingMessageConsumer, @Autowired DneProducer dneProducer,
            @Autowired String replyTo)
    {
        return new AmqpNodeService(delegatingMessageConsumer, dneProducer, replyTo, repository, discoveryInfoToVCenterDomainTransformer,
                scaleIORestToScaleIODomainTransformer, storagePoolEssRequestTransformer);
    }

    /**
     * This returns the workflow service that is used to control the add node.
     *
     * @return The workflow service that is used to control the add node.
     * @since 1.0
     */
    @Bean("addNodeWorkflowService")
    public WorkflowService addNodeWorkflowService(@Qualifier("addNodeWorkflowSteps") Map<String, Step> workflowSteps)
    {
        return new WorkflowServiceImpl(jobRepository, workflowSteps);
    }

    @Bean("preProcessWorkflowService")
    public WorkflowService preProcessWorkflowService(@Qualifier("preProcessWorkflowSteps") Map<String, Step> workflowSteps)
    {
        return new WorkflowServiceImpl(jobRepository, workflowSteps);
    }

    @Bean("dneTaskExecutorService")
    public ExecutorService dneTaskExecutorService()
    {
        return Executors.newFixedThreadPool(threadCount, new ThreadFactory()
        {
            private final AtomicInteger idCounter = new AtomicInteger();
            private static final String THREAD_NAME = "dne-task-executor-thread-%s";

            @Override
            public Thread newThread(final Runnable runnable)
            {
                return new Thread(() -> {
                    runnable.run();
                }, String.format(THREAD_NAME, idCounter.incrementAndGet()));
            }
        });
    }

    @Bean("addNodesToSystemDefinitionTaskExecutorService")
    public ExecutorService addNodesToSystemDefinitionTaskExecutorService()
    {
        return Executors.newSingleThreadExecutor(new ThreadFactory()
        {
            private final AtomicInteger idCounter = new AtomicInteger();
            private static final String THREAD_NAME = "add-nodes-to-sys-def-task-executor-thread-%s";

            @Override
            public Thread newThread(final Runnable runnable)
            {
                return new Thread(() -> {
                    runnable.run();
                }, String.format(THREAD_NAME, idCounter.incrementAndGet()));
            }
        });
    }

    @Bean
    @Scope("prototype")
    public IpAddressValidator ipAddressValidator(DataServiceRepository repository)
    {
        return new IpAddressValidator(repository);
    }
}
