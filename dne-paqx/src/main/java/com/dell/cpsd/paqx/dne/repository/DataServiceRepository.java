/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.repository;

import com.dell.cpsd.paqx.dne.domain.ComponentDetails;
import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.paqx.dne.domain.node.NodeInventory;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOData;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOProtectionDomain;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOStoragePool;
import com.dell.cpsd.paqx.dne.domain.vcenter.Host;
import com.dell.cpsd.paqx.dne.domain.vcenter.PciDevice;
import com.dell.cpsd.paqx.dne.domain.vcenter.PortGroup;
import com.dell.cpsd.paqx.dne.domain.vcenter.VCenter;
import com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds;

import java.util.List;
import java.util.Map;

/**
 * Data service repository interface
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * </p>
 *
 * @version 1.0
 * @since 1.0
 */
public interface DataServiceRepository
{
    boolean saveScaleIoComponentDetails(List<ComponentDetails> componentEndpointDetailsList);

    boolean saveVCenterComponentDetails(List<ComponentDetails> componentEndpointDetailsList);

    /**
     * This method can be used for the MVP, fetches the first
     * component, endpoint, and credential from the list.
     *
     * @param componentType VCenter/ScaleIO
     * @return ComponentEndpointIds
     */
    ComponentEndpointIds getComponentEndpointIds(String componentType);

    /**
     * This method can be used for the MVP, fetches the specified
     * component, with the specified endpoint, and its credentials.
     *
     * @param componentType
     * @param endpointType
     * @param credentialName
     * @return ComponentEndpointIds
     */
    ComponentEndpointIds getComponentEndpointIds(String componentType, String endpointType, String credentialName);

    /**
     * This method returns the component endpoint uuids based on
     * the endpoint type defined in the System Definition File.
     *
     * @param endpointType VCENTER-CUSTOMER/VCENTER-MANAGEMENT
     * @return ComponentEndpointIds
     */
    ComponentEndpointIds getVCenterComponentEndpointIdsByEndpointType(String endpointType);

    boolean saveVCenterData(String jobId, VCenter vCenterData);

    boolean saveScaleIoData(String jobId, ScaleIOData scaleIOData);

    Host getVCenterHost(String hostName);

    List<Host> getVCenterHosts();

    List<String> getDnsServers();

    Host getExistingVCenterHost();

    List<PortGroup> getPortGroups();

    ScaleIOData getScaleIoDataByJobId(String jobId);

    /**
     * The MVP Approach, later can be integrated with the Job,
     * when multiple discoveries will be supported.
     *
     * @return ScaleIO Data
     */
    ScaleIOData getScaleIoData();

    List<ScaleIOProtectionDomain> getScaleIoProtectionDomains();

    List<PciDevice> getPciDeviceList();

    String getClusterId(String clusterName);

    String getDataCenterName(String clusterName);

    String getVlanIdVmk0();

    boolean saveNodeInventory(NodeInventory nodeInventory);

    NodeInventory getNodeInventory(String symphonyUUID);

    List<NodeInventory> getNodeInventory();

    String getDomainName();

    boolean saveDiscoveredNodeInfo(DiscoveredNodeInfo discoveredNodeInfo);

    DiscoveredNodeInfo getDiscoveredNodeInfo(String uuid);

    List<DiscoveredNodeInfo> getDiscoveredNodeInfo();

    Map<String, String> getDvSwitchNames();

    Map<String, String> getDvPortGroupNames(Map<String, String> dvSwitchMap);

    Map<String, String> getScaleIoNetworkNames(Map<String, String> switchNames);

    ScaleIOStoragePool createStoragePool(String proptectionDomainId, String storagePoolId, String storagePoolName);

    ScaleIOProtectionDomain createProtectionDomain(String jobId, String protectionDomainId, String protectionDomainName);

    boolean cleanInMemoryDatabase();

    List<String> getAllIpAddresses();
}
