/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOData;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOProtectionDomain;
import com.dell.cpsd.paqx.dne.domain.vcenter.HostStorageDevice;
import com.dell.cpsd.paqx.dne.service.NodeService;
import com.dell.cpsd.paqx.dne.service.delegates.model.NodeDetail;
import com.dell.cpsd.paqx.dne.util.NodeInventoryParsingUtil;
import com.dell.cpsd.service.common.client.exception.ServiceExecutionException;
import com.dell.cpsd.service.common.client.exception.ServiceTimeoutException;
import com.dell.cpsd.service.engineering.standards.Device;
import com.dell.cpsd.service.engineering.standards.DeviceAssignment;
import com.dell.cpsd.service.engineering.standards.EssValidateStoragePoolResponseMessage;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAIL;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.SELECT_STORAGE_POOLS_FAILED;

@Component
@Scope("prototype")
@Qualifier("selectStoragePools")
/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
public class SelectStoragePools extends BaseWorkflowDelegate
{
    /**
     * Default name for new storage pool
     */
    private static final String DEFAULT_STORAGE_POOL_NAME = "temp";

    /**
     * ScaleIO gateway credential components
     */
    private static final String COMPONENT_TYPE = "SCALEIO-CLUSTER";

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectStoragePools.class);
    private final NodeService nodeService;

    @Autowired
    public SelectStoragePools(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        LOGGER.info("In selectStoragePools");

        NodeDetail nodeDetail = (NodeDetail) delegateExecution.getVariable(NODE_DETAIL);
        final String symphonyUuid = nodeDetail.getId();

        Map<String, DeviceAssignment> deviceMap = new HashMap<>();

        try
        {
            List<Device> newDevices = getNewDevices(symphonyUuid);

            if (CollectionUtils.isEmpty(newDevices))
            {
                String message = "No disks found in the node inventory data.";
                updateDelegateStatus(message);
                LOGGER.error(message);
                throw new BpmnError(SELECT_STORAGE_POOLS_FAILED, message);
            }

            // retrieve scale IO data
            List<ScaleIOData> scaleIODataList = nodeService.listScaleIOData();

            // retrieve vCenter data
            Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap = nodeService
                    .getHostToStorageDeviceMap(nodeService.findVcenterHosts());

            ScaleIOData scaleIOData = scaleIODataList.get(0);

            List<ScaleIOProtectionDomain> protectionDomains = scaleIOData.getProtectionDomains();
            if (protectionDomains != null)
            {
                validateStoragePoolsAndSetResponse(deviceMap, newDevices, hostToStorageDeviceMap, protectionDomains,
                        nodeDetail.getProtectionDomainId());
            }

            nodeDetail.setDeviceToDeviceStoragePool(deviceMap);
            delegateExecution.setVariable(NODE_DETAIL, nodeDetail);
        }
        catch (Exception ex)
        {
            LOGGER.error("Error finding or creating a valid storage pool", ex);
            updateDelegateStatus("Error finding or creating a valid storage pool " + ex.getMessage());
            throw new BpmnError(SELECT_STORAGE_POOLS_FAILED, ex.getMessage());
        }
    }

    /**
     * Validate if the existing storage pools are valid, if not create new one.
     *
     * @param deviceMap              Response back to the client
     * @param newDevices             Device list from new node inventory
     * @param hostToStorageDeviceMap Map consisting of host name : displayName : HostStorageDevice
     * @param protectionDomains      Protection domains from current scale io data
     * @param protectionDomainId     Curent DNE job
     * @throws ServiceTimeoutException
     * @throws ServiceExecutionException
     */
    private void validateStoragePoolsAndSetResponse(Map<String, DeviceAssignment> deviceMap, final List<Device> newDevices,
            final Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap, final List<ScaleIOProtectionDomain> protectionDomains,
            final String protectionDomainId) throws ServiceTimeoutException, ServiceExecutionException
    {
        if (StringUtils.isEmpty(protectionDomainId))
        {
            throw new IllegalStateException("Protection domain id is null");
        }

        final ScaleIOProtectionDomain scaleIOProtectionDomain = protectionDomains.stream().filter(Objects::nonNull)
                .filter(protectionDomain -> protectionDomain.getId().equals(protectionDomainId)).findFirst().orElse(null);

        if (scaleIOProtectionDomain == null)
        {
            throw new IllegalStateException("Could not find a valid protection domain");
        }

        if (!findValidStoragePool(deviceMap, newDevices, hostToStorageDeviceMap, scaleIOProtectionDomain))
        {
            //If we do not find a storage pool, then for now we assume it's going to be a newly-created
            //"temp" storage pool and the devices will be assigned to it.
            newDevices.stream()
                    .filter(Objects::nonNull)
                    .forEach(device->deviceMap.put(device.getId(), new DeviceAssignment(device.getId(), device.getSerialNumber(), device.getLogicalName(), device.getName(),null, "temp")));
        }
    }

    /**
     * Finds if the storage pools within the protection domain is valid
     *
     * @param deviceMap              Response back to the client
     * @param newDevices             Device list from new node inventory
     * @param hostToStorageDeviceMap Map consisting of host name : displayName : HostStorageDevice
     * @param protectionDomain       Protection domains from current scale io data
     * @return true if the response is successful else false
     * @throws ServiceTimeoutException
     * @throws ServiceExecutionException
     */
    private boolean findValidStoragePool(Map<String, DeviceAssignment> deviceMap, final List<Device> newDevices,
            final Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap, ScaleIOProtectionDomain protectionDomain)
            throws ServiceTimeoutException, ServiceExecutionException
    {
        EssValidateStoragePoolResponseMessage storageResponseMessage = nodeService
                .validateStoragePools(protectionDomain.getStoragePools(), newDevices, hostToStorageDeviceMap);

        storageResponseMessage.getWarnings().forEach(f -> updateDelegateStatus(f.getMessage()));

        // if all devices are allocated to existing pools or some of the disks already allocated or < 90GB and no errors
        if (CollectionUtils.size(storageResponseMessage.getDeviceToStoragePoolMap()) == CollectionUtils.size(newDevices) || CollectionUtils
                .isEmpty(storageResponseMessage.getErrors()))
        {
            String message = "Storage pool validated successfully.";
            LOGGER.info(message);
            deviceMap.putAll(storageResponseMessage.getDeviceToStoragePoolMap());
            updateDelegateStatus(message);
            return true;
        }

        storageResponseMessage.getErrors().forEach(f -> updateDelegateStatus(f.getMessage()));
        return false;
    }

    public List<Device> getNewDevices(String symphonyUuid)
    {
        return NodeInventoryParsingUtil.parseNewDevices(nodeService.getNodeInventoryData(symphonyUuid));
    }
}