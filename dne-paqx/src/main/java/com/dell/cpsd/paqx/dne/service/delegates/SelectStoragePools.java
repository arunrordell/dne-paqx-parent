/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.delegates;

import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.paqx.dne.domain.node.NodeInventory;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOData;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOProtectionDomain;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOStoragePool;
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
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.NODE_DETAILS;
import static com.dell.cpsd.paqx.dne.service.delegates.utils.DelegateConstants.SELECT_STORAGE_POOLS_FAILED;

/**
 * Task responsible for selecting storage pool(s) for nodes
 * <p>
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */
@Component
@Scope("prototype")
@Qualifier("selectStoragePools")
public class SelectStoragePools extends BaseWorkflowDelegate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectStoragePools.class);
    private final NodeService nodeService;
    private static final String STORAGE_POOL_PREFIX = "temp-";

    @Autowired
    public SelectStoragePools(NodeService nodeService)
    {
        super(LOGGER, "Select Storage Pools");
        this.nodeService = nodeService;
    }

    @Override
    public void delegateExecute(final DelegateExecution delegateExecution)
    {
        try
        {
            List<NodeDetail> nodeDetails = (List<NodeDetail>) delegateExecution.getVariable(NODE_DETAILS);
            if (CollectionUtils.isEmpty(nodeDetails))
            {
                throw new IllegalStateException(
                        "The List of Node Detail was not found!  Please add at least one Node Detail and try again.");
            }

            Map<String, List<Device>> protectionDomainToDevicesMap = new HashMap<>();
            Map<String, List<Device>> nodeToDeviceMap = new HashMap<>();

            // this is used to determine the type of drives (SSD or HDD), for nodes previously added by DNE workflow
            // as this information is not available in vcenter
            Map<String, Device.Type> deviceTypeMap = getDeviceInfoForAllNodes();

            populateDeviceMaps(nodeDetails, nodeToDeviceMap, protectionDomainToDevicesMap);

            if (protectionDomainToDevicesMap.values().stream().mapToInt(Collection::size).sum() == 0)
            {
                throw new IllegalStateException("No disks found in the node inventory data.");
            }

            // retrieve vCenter data
            Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap = nodeService
                    .getHostToStorageDeviceMap(nodeService.findVcenterHosts());

            // retrieve scale IO data
            List<ScaleIOData> scaleIODataList = nodeService.listScaleIOData();

            ScaleIOData scaleIOData = scaleIODataList.get(0);

            List<ScaleIOProtectionDomain> protectionDomains = scaleIOData.getProtectionDomains();

            Map<String, DeviceAssignment> deviceMap = new HashMap<>();

            if (protectionDomains != null)
            {
                // call ESS based on protection domain and corresponding devices
                protectionDomainToDevicesMap.entrySet().stream().forEach(pd -> {
                    try
                    {
                        validateStoragePoolsAndSetResponse(deviceMap, pd.getValue(), hostToStorageDeviceMap, protectionDomains, pd.getKey(),
                                deviceTypeMap);
                    }
                    catch (ServiceTimeoutException | ServiceExecutionException e)
                    {
                        throw new IllegalStateException("Error validating storage pool(s) for protection domain: " + pd.getKey());
                    }
                });
            }
            setDeviceMapToNodeDetail(nodeDetails, nodeToDeviceMap, deviceMap);
        }
        catch (Exception ex)
        {
            updateDelegateStatus("Error finding valid storage pool(s): " + ex.getMessage(), ex);
            throw new BpmnError(SELECT_STORAGE_POOLS_FAILED, ex.getMessage());
        }
    }

    protected void setDeviceMapToNodeDetail(final List<NodeDetail> nodeDetails, final Map<String, List<Device>> nodeToDeviceMap,
            final Map<String, DeviceAssignment> deviceMap)
    {
        // set devices to storage pool map back to node
        nodeDetails.stream().forEach(nodeDetail -> {
            Map<String, DeviceAssignment> deviceAssignmentMap = new HashMap<>();
            List<Device> nodeDevices = nodeToDeviceMap.get(nodeDetail.getId());
            nodeDevices.stream().filter(Objects::nonNull).forEach(device -> {
                deviceAssignmentMap.put(device.getId(), deviceMap.get(device.getId()));
            });
            nodeDetail.setDeviceToDeviceStoragePool(deviceAssignmentMap);
        });
    }

    protected void populateDeviceMaps(final List<NodeDetail> nodeDetails, final Map<String, List<Device>> nodeToDeviceMap,
            final Map<String, List<Device>> protectionDomainToDevicesMap)
    {
        // separate devices based on protection domain, as calls to ESS are based on protection domain
        nodeDetails.stream().forEach(nodeDetail -> {
            if (nodeDetail.getProtectionDomainId() == null)
            {
                throw new IllegalStateException("Could not find a valid protection domain for node: " + nodeDetail.getId());
            }
            String symphonyUuid = nodeDetail.getId();

            List<Device> newDevices = getNewDevices(symphonyUuid);
            nodeToDeviceMap.put(symphonyUuid, newDevices);

            if (protectionDomainToDevicesMap.get(nodeDetail.getProtectionDomainId()) == null)
            {
                protectionDomainToDevicesMap.put(nodeDetail.getProtectionDomainId(), new ArrayList<Device>());
            }
            protectionDomainToDevicesMap.get(nodeDetail.getProtectionDomainId())
                    .addAll(newDevices != null ? newDevices : Collections.emptyList());
        });
    }

    protected Map<String, Device.Type> getDeviceInfoForAllNodes()
    {
        Map<String, Device.Type> deviceTypeMap = new HashMap<>();
        List<NodeInventory> nodeInventoryList = nodeService.getNodeInventoryDataForAllNodes();
        nodeInventoryList.stream().filter(Objects::nonNull).forEach(nodeInventory -> {
            List<Device> newDevices = NodeInventoryParsingUtil.parseNewDevices(nodeInventory.getNodeInventory());
            newDevices.stream().filter(Objects::nonNull).forEach(newDevice -> {
                deviceTypeMap.put(newDevice.getId(), newDevice.getType());
            });
        });
        return deviceTypeMap;
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
    protected void validateStoragePoolsAndSetResponse(Map<String, DeviceAssignment> deviceMap, final List<Device> newDevices,
            final Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap, final List<ScaleIOProtectionDomain> protectionDomains,
            final String protectionDomainId, Map<String, Device.Type> deviceTypeMap)
            throws ServiceTimeoutException, ServiceExecutionException
    {

        final ScaleIOProtectionDomain scaleIOProtectionDomain = protectionDomains.stream().filter(Objects::nonNull)
                .filter(protectionDomain -> protectionDomain.getId().equals(protectionDomainId)).findFirst().orElse(null);

        if (scaleIOProtectionDomain == null)
        {
            throw new IllegalStateException("Could not find a valid protection domain");
        }

        ScaleIOProtectionDomain dummyProtectionDomains = copyFromEntityToDomain(scaleIOProtectionDomain);

        // ESS has to be called N number of times (to a max of 5 times), as we do not really know how many new storage pools are
        // really required
        int numberOfIterations = 1;
        int storagePoolNameCounter = 1;
        String storagePoolName = STORAGE_POOL_PREFIX + storagePoolNameCounter;
        while (!findValidStoragePool(deviceMap, newDevices, hostToStorageDeviceMap, dummyProtectionDomains, deviceTypeMap))
        {
            // max 6 attempts to ensure all drives are assigned
            if (numberOfIterations >= 6)
            {
                break;
            }

            // ensure the name used for dummy pool is not already used, max 5 pools to be created
            while (protectionDomainContainsStoragePoolName(dummyProtectionDomains, storagePoolName))
            {
                storagePoolNameCounter++;
                storagePoolName = STORAGE_POOL_PREFIX + storagePoolNameCounter;
            }
            LOGGER.info("Allocating dummy storage pool with name, " + storagePoolName);

            // create a dummy storage pool and add it to the pool list and see if it is enough
            ScaleIOStoragePool storagePool = new ScaleIOStoragePool();
            storagePool.setUseRfcache(false);
            storagePool.setUseRmcache(false);
            storagePool.setZeroPaddingEnabled(true);
            storagePool.setId(null);
            storagePool.setName(storagePoolName);
            dummyProtectionDomains.addStoragePool(storagePool);
            numberOfIterations++;
        }
    }

    private boolean protectionDomainContainsStoragePoolName(ScaleIOProtectionDomain scaleIOProtectionDomain, String storagePoolName)
    {
        return scaleIOProtectionDomain.getStoragePools().stream().filter(Objects::nonNull)
                .filter(sp -> storagePoolName.equalsIgnoreCase(sp.getName())).findAny().isPresent();
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
            final Map<String, Map<String, HostStorageDevice>> hostToStorageDeviceMap, ScaleIOProtectionDomain protectionDomain,
            Map<String, Device.Type> deviceTypeMap) throws ServiceTimeoutException, ServiceExecutionException
    {
        EssValidateStoragePoolResponseMessage storageResponseMessage = nodeService
                .validateStoragePools(protectionDomain.getStoragePools(), newDevices, hostToStorageDeviceMap, deviceTypeMap);
        LOGGER.info("Response from ESS: " + storageResponseMessage);

        storageResponseMessage.getWarnings().forEach(f -> updateDelegateStatus(f.getMessage()));

        // if all devices are allocated to existing pools OR some of the disks already allocated or < 90GB and no errors
        if (CollectionUtils.size(storageResponseMessage.getDeviceToStoragePoolMap()) == CollectionUtils.size(newDevices) || CollectionUtils
                .isEmpty(storageResponseMessage.getErrors()))
        {
            deviceMap.putAll(storageResponseMessage.getDeviceToStoragePoolMap());
            updateDelegateStatus("Storage pool validated successfully.");
            return true;
        }
        LOGGER.info("Valid storage pool not found.");
        storageResponseMessage.getErrors().forEach(f -> updateDelegateStatus(f.getMessage()));
        return false;
    }

    protected List<Device> getNewDevices(String symphonyUuid)
    {
        return NodeInventoryParsingUtil.parseNewDevices(nodeService.getNodeInventoryData(symphonyUuid));
    }

    private ScaleIOProtectionDomain copyFromEntityToDomain(ScaleIOProtectionDomain scaleIOProtectionDomain)
    {
        ScaleIOProtectionDomain newScaleIOProtectionDomain = new ScaleIOProtectionDomain();
        newScaleIOProtectionDomain.setId(scaleIOProtectionDomain.getId());
        scaleIOProtectionDomain.getStoragePools().stream().forEach(sp -> {
            ScaleIOStoragePool storagePool = new ScaleIOStoragePool();
            storagePool.setName(sp.getName());
            storagePool.setId(sp.getId());
            storagePool.setDevices(sp.getDevices());
            storagePool.setUseRmcache(sp.isUseRmcache());
            storagePool.setUseRfcache(sp.isUseRfcache());
            storagePool.setZeroPaddingEnabled(sp.isZeroPaddingEnabled());
            newScaleIOProtectionDomain.addStoragePool(storagePool);
        });
        return newScaleIOProtectionDomain;
    }
}