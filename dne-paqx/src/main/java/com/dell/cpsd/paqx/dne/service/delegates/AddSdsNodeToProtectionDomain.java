/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.exception.TaskResponseFailureException;
import com.dell.cpsd.paqx.dne.repository.DataServiceRepository;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.service.engineering.standards.DeviceAssignment;
import com.dell.cpsd.storage.capabilities.api.AddHostToProtectionDomainRequestMessage;
import com.dell.cpsd.storage.capabilities.api.DeviceInfo;
import com.dell.cpsd.storage.capabilities.api.HostToProtectionDomain;
import com.dell.cpsd.storage.capabilities.api.SdsIp;
import com.dell.cpsd.storage.capabilities.api.SdsIpDetails;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.ADD_VCENTER_HOST_TO_PROTECTION_DOMAIN;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;

/**
 * Add a vCenter host to a protection domain.
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @version 1.0
 * @since 1.0
 */
@Component
@Scope("prototype")
@Qualifier("addVCenterHostToProtectionDomain")
//TODO: Change camunda delete expression and move logic to transformer
public class AddSdsNodeToProtectionDomain extends BaseWorkflowDelegate
{
    /**
     * The logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AddSdsNodeToProtectionDomain.class);

    /**
     * The <code>NodeService</code> instance
     */
    private NodeService nodeService;

    private final DataServiceRepository repository;

    @Autowired
    public AddSdsNodeToProtectionDomain(final NodeService nodeService, final DataServiceRepository repository)
    {
        this.nodeService = nodeService;
        this.repository = repository;
    }

    /**
     * get Sds names
     *
     * @param nodeDetail
     * @return
     */
    private String getSdsName(NodeDetail nodeDetail)
    {
        String name = null;
        String esxiManagementHostname = nodeDetail.getEsxiManagementHostname();
        if (esxiManagementHostname != null)
        {
            String hostDomain = repository.getDomainName();
            if (!esxiManagementHostname.contains(hostDomain))
            {
                esxiManagementHostname = esxiManagementHostname + "." + hostDomain;
            }
            name = (esxiManagementHostname + "-ESX");
        }
        else if (nodeDetail.getEsxiManagementIpAddress() != null)
        {
            name = (nodeDetail.getEsxiManagementIpAddress() + "-ESX");
        }

        return name;
    }

    /**
     * Create Sds Ip list from input
     *
     * @param nodeDetail
     * @return
     */
    private List<SdsIp> createSdsIps(NodeDetail nodeDetail)
    {
        String ScaleIoData1IP = nodeDetail.getScaleIoData1SvmIpAddress();
        String ScaleIoData2IP = nodeDetail.getScaleIoData2SvmIpAddress();

        SdsIp sdsIp1 = new SdsIp();
        SdsIpDetails sdsIpDetails1 = new SdsIpDetails();
        sdsIpDetails1.setIp(ScaleIoData1IP);
        sdsIpDetails1.setRole(SdsIpDetails.Role.ALL);
        sdsIp1.setSdsIpDetails(sdsIpDetails1);

        SdsIp sdsIp2 = new SdsIp();
        SdsIpDetails sdsIpDetails2 = new SdsIpDetails();
        sdsIpDetails2.setIp(ScaleIoData2IP);
        sdsIpDetails2.setRole(SdsIpDetails.Role.ALL);
        sdsIp2.setSdsIpDetails(sdsIpDetails2);

        List<SdsIp> sdsIpList = new ArrayList<>();
        sdsIpList.add(0, sdsIp1);
        sdsIpList.add(1, sdsIp2);
        return sdsIpList;
    }

    /**
     * Create Host to protection domain
     *
     * @param protectionDomain
     * @param name
     * @param sdsIpList
     * @param deviceInfoList
     * @return
     */
    private HostToProtectionDomain createHostToProtectionDomain(String protectionDomain, String name, List<SdsIp> sdsIpList,
            final List<DeviceInfo> deviceInfoList)
    {
        HostToProtectionDomain hostToProtectionDomain = new HostToProtectionDomain();
        hostToProtectionDomain.setProtectionDomainId(protectionDomain);
        hostToProtectionDomain.setName(name);
        hostToProtectionDomain.setSdsIpList(sdsIpList);
        hostToProtectionDomain.setDeviceInfoList(deviceInfoList);

        return hostToProtectionDomain;
    }

    private List<DeviceInfo> createDeviceInfoList(Map<String, DeviceAssignment> deviceToDeviceStoragePoolAssignment)
    {

        List<DeviceInfo> returnVal = null;

        if (deviceToDeviceStoragePoolAssignment != null)
        {
            //spaces are NOT allowed in the deviceName, so we will replace any spaces with '/'
            // '/' is a valid character for deviceName
            //the deviceName tends to look like "/dev/sda scsi" so it will change to "/dev/sda/scsi"
            returnVal = deviceToDeviceStoragePoolAssignment.values().stream().filter(Objects::nonNull)
                    .map(ddspa -> new DeviceInfo(ddspa.getLogicalName(), ddspa.getStoragePoolId(), null)).collect(Collectors.toList());
        }
        return returnVal;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        LOGGER.info("Execute AddVCenterHostToProtectionDomain task");
        final String taskMessage = "Add Host To Protection Domain";
        final NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);

        AddHostToProtectionDomainRequestMessage requestMessage = new AddHostToProtectionDomainRequestMessage();

        final String protectionDomainId = nodeDetail.getProtectionDomainId();

        final Map<String, DeviceAssignment> deviceToDeviceStoragePoolAssignment = nodeDetail.getDeviceToDeviceStoragePool();

        String name = getSdsName(nodeDetail);

        List<SdsIp> sdsIpList = createSdsIps(nodeDetail);

        List<DeviceInfo> deviceInfoList = createDeviceInfoList(deviceToDeviceStoragePoolAssignment);

        final com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds componentEndpointIds = repository
                .getComponentEndpointIds("SCALEIO-CLUSTER");

        com.dell.cpsd.storage.capabilities.api.ComponentEndpointIds componentEndpoints = new com.dell.cpsd.storage.capabilities.api.ComponentEndpointIds();
        componentEndpoints.setEndpointUuid(componentEndpointIds.getEndpointUuid());
        componentEndpoints.setComponentUuid(componentEndpointIds.getComponentUuid());
        componentEndpoints.setCredentialUuid(componentEndpointIds.getCredentialUuid());

        HostToProtectionDomain hostToProtectionDomain = createHostToProtectionDomain(protectionDomainId, name, sdsIpList, deviceInfoList);

        String endpointUrl = ("https://" + componentEndpointIds.getEndpointUrl() + ":443");
        requestMessage.setHostToProtectionDomain(hostToProtectionDomain);
        requestMessage.setComponentEndpointIds(componentEndpoints);
        requestMessage.setEndpointUrl(endpointUrl);

        try
        {
            this.nodeService.requestAddHostToProtectionDomain(requestMessage);

            final String returnMessage = taskMessage + " on Node " + nodeDetail.getServiceTag() + " was successful.";
            LOGGER.info(returnMessage);
            updateDelegateStatus(returnMessage);
        }
        catch (TaskResponseFailureException ex)
        {
            updateDelegateStatus(ex.getMessage());
            throw new BpmnError(ADD_VCENTER_HOST_TO_PROTECTION_DOMAIN, "Exception Code: " + ex.getCode() + "::" + ex.getMessage());
        }
        catch (Exception e)
        {
            String errorMessage = "An Unexpected Exception occurred attempting to request " + taskMessage + ".  Reason: ";
            LOGGER.error(errorMessage, e);
            updateDelegateStatus(errorMessage + e.getMessage());
            throw new BpmnError(ADD_VCENTER_HOST_TO_PROTECTION_DOMAIN, errorMessage + e.getMessage());
        }
    }
}
