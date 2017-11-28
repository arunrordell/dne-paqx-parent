/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
package com.dell.cpsd.paqx.dne.service.delegates.request;

import com.dell.cpsd.EsxiInstallationInfo;
import com.dell.cpsd.paqx.dne.amqp.callback.AsynchronousNodeServiceCallback;
import com.dell.cpsd.paqx.dne.service.AsynchronousNodeService;
import com.dell.cpsd.paqx.dne.service.delegates.BaseWorkflowDelegate;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.service.model.IpV4Configuration;
import com.dell.cpsd.paqx.dne.transformers.HostToInstallEsxiRequestTransformer;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.INSTALL_ESXI_MESSAGE_ID;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.SEND_INSTALL_ESXI_FAILED;

@Component
@Scope("prototype")
@Qualifier("sendInstallEsxiRequest")
public class SendInstallEsxiRequest extends BaseWorkflowDelegate
{
    /**
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SendInstallEsxiRequest.class);

    /**
     * The <code>NodeService</code> instance
     */
    private final AsynchronousNodeService asynchronousNodeService;

    private final HostToInstallEsxiRequestTransformer hostToInstallEsxiRequestTransformer;


    @Autowired
    public SendInstallEsxiRequest(final AsynchronousNodeService asynchronousNodeService,
                                  final HostToInstallEsxiRequestTransformer hostToInstallEsxiRequestTransformer)
    {
        this.asynchronousNodeService = asynchronousNodeService;
        this.hostToInstallEsxiRequestTransformer = hostToInstallEsxiRequestTransformer;
    }

    /*
    * Auto generates the hostname using the ESXI Management IP Address.
    */
    private String generateHostname(final String esxiManagementIpAddress)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("vCenterHost");
        builder.append("-");
        builder.append(esxiManagementIpAddress.replaceAll("\\.", "-"));
        return builder.toString();
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {

        LOGGER.info("Execute Send Install ESXi Request");
        final String taskMessage = "The Request to Send Install ESXi";
        NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);

        final String symphonyUuid = nodeDetail.getId();

        final String esxiManagementIpAddress = nodeDetail.getEsxiManagementIpAddress();

        final String esxiManagementGatewayIpAddress = nodeDetail.getEsxiManagementGatewayIpAddress();

        final String esxiManagementSubnetMask = nodeDetail.getEsxiManagementSubnetMask();

        String esxiManagementHostname = nodeDetail.getEsxiManagementHostname();
        if (StringUtils.isEmpty(esxiManagementHostname))
        {
            LOGGER.warn("ESXi Management hostname is null, will auto generate hostname");
            esxiManagementHostname = this.generateHostname(esxiManagementIpAddress);
            nodeDetail.setEsxiManagementHostname(esxiManagementHostname);
            delegateExecution.setVariable(NODE_DETAIL, nodeDetail);
            LOGGER.info("Auto generated ESXi Management hostname is " + esxiManagementHostname);
        }


        final IpV4Configuration ipV4Configuration = new IpV4Configuration();
        ipV4Configuration.setEsxiManagementIpAddress(esxiManagementIpAddress);
        ipV4Configuration.setEsxiManagementGateway(esxiManagementGatewayIpAddress);
        ipV4Configuration.setEsxiManagementNetworkMask(esxiManagementSubnetMask);

        final EsxiInstallationInfo esxiInstallationInfo = hostToInstallEsxiRequestTransformer.transformInstallEsxiData(esxiManagementHostname,
                                                                                                                       symphonyUuid,
                                                                                                                       ipV4Configuration);

        AsynchronousNodeServiceCallback<?> requestCallback = null;
        try
        {
            requestCallback = this.asynchronousNodeService.requestInstallEsxi(delegateExecution.getProcessInstanceId(),
                                                                              "receiveInstallEsxiResponse",
                                                                              INSTALL_ESXI_MESSAGE_ID,
                                                                              esxiInstallationInfo);
        }
        catch (Exception e)
        {
            final String message =
                    "An Unexpected Exception Occurred attempting to Install Esxi on Node " + nodeDetail.getServiceTag();
            LOGGER.error(message, e);
            updateDelegateStatus(message);
            throw new BpmnError(SEND_INSTALL_ESXI_FAILED,
                                taskMessage + " on Node " + nodeDetail.getServiceTag() + " failed!  Reason: " +
                                e.getMessage());
        }

        if (requestCallback != null)
        {
            final String message = taskMessage + " on Node " + nodeDetail.getServiceTag() + " was successful";
            LOGGER.info(message);
            updateDelegateStatus(message);
        }
        else
        {
            final String message = "Failed to send the request for Install ESXi on Node " + nodeDetail.getServiceTag();
            LOGGER.error(message);
            updateDelegateStatus(message);
            throw new BpmnError(SEND_INSTALL_ESXI_FAILED, message);
        }
    }
}
