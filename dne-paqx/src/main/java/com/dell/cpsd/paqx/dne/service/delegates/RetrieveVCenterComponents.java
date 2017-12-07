/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.service.NodeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.RETRIEVE_VCENTER_COMPONENTS_FAILED;

@Component
@Scope("prototype")
@Qualifier("retrieveVCenterComponents")
public class RetrieveVCenterComponents extends BaseWorkflowDelegate
{
    /**
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RetrieveVCenterComponents.class);

    /*
     * The <code>NodeService</code> instance
     */
    private final NodeService nodeService;

    @Autowired
    public RetrieveVCenterComponents(final NodeService nodeService)
    {
        super(LOGGER, "List VCenter Components");
        this.nodeService = nodeService;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        try
        {
            this.nodeService.requestVCenterComponents();

            updateDelegateStatus("VCenter Components were retrieved successfully.");
        }
        catch (Exception e)
        {
            final String message = "An Unexpected Exception occurred while retrieving VCenter Components. Reason: ";
            updateDelegateStatus(message, e);
            throw new BpmnError(RETRIEVE_VCENTER_COMPONENTS_FAILED, message + e.getMessage());
        }
    }
}
