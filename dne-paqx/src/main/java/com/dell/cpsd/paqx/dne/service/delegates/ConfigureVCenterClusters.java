/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.NodeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Qualifier("configureVCenterClusters")
public class ConfigureVCenterClusters extends BaseWorkflowDelegate
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureVCenterClusters.class);

    private NodeService nodeService;

    @Autowired
    public ConfigureVCenterClusters(final NodeService nodeService)
    {
        super(LOGGER, "Validate VCenter Clusters");
        this.nodeService = nodeService;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {

    }
}
