/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.CLEAN_IN_MEMORY_DATABASE_ERROR;

/**
 * It is responsible for cleaning in memory database.
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @version 1.0
 * @since 1.0
 */
@Component
@Scope("prototype")
@Qualifier("cleanInMemoryDatabase")
public class CleanInMemoryDatabase extends BaseWorkflowDelegate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanInMemoryDatabase.class);

    /*
     * The <code>DataServiceRepository</code> instance
     */
    private final DataServiceRepository repository;

    @Autowired
    public CleanInMemoryDatabase(final DataServiceRepository repository)
    {
        super(LOGGER, "Cleaning up the Database");
        this.repository = repository;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        updateDelegateStatus(this.taskName + ".");
        boolean result;
        try
        {
            result = repository.cleanInMemoryDatabase();
        }
        catch (Exception ex)
        {
            String errorMessage = "An Unexpected Error occurred while " + taskName + ". Reason: ";
            updateDelegateStatus(errorMessage, ex);
            throw new BpmnError(CLEAN_IN_MEMORY_DATABASE_ERROR, errorMessage + ex.getMessage());
        }

        if(!result) {
            final String errorMessage = taskName + " failed.";
            updateDelegateStatus(errorMessage);
            throw new BpmnError(CLEAN_IN_MEMORY_DATABASE_ERROR, errorMessage);
        }
        updateDelegateStatus(taskName + " was successful.");
    }
}
