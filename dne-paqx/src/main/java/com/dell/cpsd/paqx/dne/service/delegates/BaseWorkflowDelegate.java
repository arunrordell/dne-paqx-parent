/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
package com.dell.cpsd.paqx.dne.service.delegates;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.DELEGATE_STATUS_VARIABLE;

/**
 * Created by Amy_Reed on 8/15/2017.
 */
public abstract class BaseWorkflowDelegate implements JavaDelegate
{

    private static final Log LOGGER = LogFactory.getLog(BaseWorkflowDelegate.class);
    private List<String> delegateStatus = new CopyOnWriteArrayList<>();

    @Override
    public void execute(final DelegateExecution delegateExecution) throws Exception
    {
        preExecute(delegateExecution);
        try
        {
            delegateExecute(delegateExecution);
        }
        catch (BpmnError bpmnError)
        {
            throw bpmnError;
        }
        catch (Exception e)
        {
            final String errorMessage = "An Unexpected Exception occurred in the workflow.";
            LOGGER.error("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            LOGGER.error(errorMessage, e);
            LOGGER.error("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            throw e;
        }
        finally
        {
            postExecute(delegateExecution);
        }
    }

    public abstract void delegateExecute(DelegateExecution delegateExecution);

    /**
     * PreExecute functionality to allow for updating status information before execution.
     * @param delegateExecution
     */
    public void preExecute(final DelegateExecution delegateExecution) {}

    public void postExecute(final DelegateExecution delegateExecution)
    {
        if (CollectionUtils.isNotEmpty(delegateStatus))
        {
            List<String> currentDelegateStatus = (List<String>) delegateExecution.getVariable(DELEGATE_STATUS_VARIABLE);
            if (currentDelegateStatus == null)
            {
                currentDelegateStatus = delegateStatus;
            }
            else
            {
                currentDelegateStatus.addAll(delegateStatus);
            }
            delegateExecution.setVariable(DELEGATE_STATUS_VARIABLE, currentDelegateStatus);
        }
    }

    public void updateDelegateStatus(String statusString) {
        delegateStatus.add(statusString);
    }
}
