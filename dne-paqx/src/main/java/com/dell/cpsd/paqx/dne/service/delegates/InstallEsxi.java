/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.HOSTNAME;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.INSTALL_ESXI_FAILED;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.INSTALL_ESXI_MESSAGE_ID;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;

@Component
@Scope("prototype")
@Qualifier("installEsxi")
public class InstallEsxi extends BaseWorkflowDelegate
{
    /**
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(InstallEsxi.class);

    private final AsynchronousNodeService asynchronousNodeService;

    private final DataServiceRepository repository;

    @Autowired
    public InstallEsxi(final AsynchronousNodeService asynchronousNodeService, final DataServiceRepository repository)
    {
        this.asynchronousNodeService = asynchronousNodeService;
        this.repository = repository;
    }

    /*
    * Builds the host fully qualified domain name (FQDN) from
    * the hostname and domain name, where the domain name is
    * retrieved from the database.
    */
    private String generateHostFqdn(String hostName)
    {
        String domainName = this.repository.getDomainName();
        if (StringUtils.isEmpty(domainName))
        {
            return hostName;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(hostName);
        builder.append(".");
        builder.append(domainName);
        return builder.toString();
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        LOGGER.info("Execute Install ESXi");
        NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);
        AsynchronousNodeServiceCallback<?> responseCallback = (AsynchronousNodeServiceCallback<?>) delegateExecution
                .getVariable(INSTALL_ESXI_MESSAGE_ID);

        String status = null;
        if (responseCallback != null && responseCallback.isDone())
        {
            try
            {
                status = this.asynchronousNodeService.requestInstallEsxi(responseCallback);
            }
            catch (Exception e)
            {
                final String message = "Install Esxi on Node " + nodeDetail.getServiceTag() + " failed!  Reason: ";
                LOGGER.error(message, e);
                updateDelegateStatus(message + e.getMessage());
                throw new BpmnError(INSTALL_ESXI_FAILED, message + e.getMessage());
            }
        }

        if (status == null || !"succeeded".equalsIgnoreCase(status))
        {
            final String message = "Install Esxi on Node " + nodeDetail.getServiceTag() + " failed!";
            LOGGER.error(message);
            updateDelegateStatus(message);
            throw new BpmnError(INSTALL_ESXI_FAILED, message);
        }

        String fqdn = this.generateHostFqdn(nodeDetail.getEsxiManagementHostname());
        delegateExecution.setVariable(HOSTNAME, fqdn);
        LOGGER.info("Install Esxi on Node " + nodeDetail.getServiceTag() + " was successful.");
        updateDelegateStatus("Install Esxi on Node " + nodeDetail.getServiceTag() + " was successful.");
    }
}
