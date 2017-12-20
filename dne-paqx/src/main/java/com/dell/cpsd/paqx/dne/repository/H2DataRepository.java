/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.repository;

import com.dell.cpsd.paqx.dne.domain.ComponentDetails;
import com.dell.cpsd.paqx.dne.domain.CredentialDetails;
import com.dell.cpsd.paqx.dne.domain.DneJob;
import com.dell.cpsd.paqx.dne.domain.EndpointDetails;
import com.dell.cpsd.paqx.dne.domain.node.DiscoveredNodeInfo;
import com.dell.cpsd.paqx.dne.domain.node.NodeInventory;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOData;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOProtectionDomain;
import com.dell.cpsd.paqx.dne.domain.scaleio.ScaleIOStoragePool;
import com.dell.cpsd.paqx.dne.domain.vcenter.Host;
import com.dell.cpsd.paqx.dne.domain.vcenter.HostDnsConfig;
import com.dell.cpsd.paqx.dne.domain.vcenter.PciDevice;
import com.dell.cpsd.paqx.dne.domain.vcenter.PortGroup;
import com.dell.cpsd.paqx.dne.domain.vcenter.VCenter;
import com.dell.cpsd.paqx.dne.service.model.ComponentEndpointIds;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * H2-InMemory Data Repository
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * </p>
 *
 * @version 1.0
 * @since 1.0
 */
@Repository
public class H2DataRepository implements DataServiceRepository
{
    private static final Logger LOG = LoggerFactory.getLogger(H2DataRepository.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public boolean saveScaleIoComponentDetails(final List<ComponentDetails> componentEndpointDetailsList)
    {
        LOG.info("Persisting ScaleIO Component, Endpoint and Credential UUID");

        if (componentEndpointDetailsList.isEmpty())
        {
            LOG.error("No Components Found");
            return false;
        }

        try
        {
            componentEndpointDetailsList.stream().filter(Objects::nonNull).forEach(componentDetails -> {
                String componentUuid = componentDetails.getComponentUuid();

                try
                {
                    TypedQuery<ComponentDetails> componentDetailsTypedQuery = entityManager
                            .createQuery("select c from ComponentDetails as c where c.componentUuid =:componentUuid",
                                    ComponentDetails.class);
                    componentDetailsTypedQuery.setParameter("componentUuid", componentUuid);

                    final List<ComponentDetails> existingComponentList = componentDetailsTypedQuery.getResultList();
                    if (existingComponentList != null && !existingComponentList.isEmpty())
                    {
                        entityManager.merge(existingComponentList.get(0));
                        entityManager.flush();
                    }
                    else
                    {
                        entityManager.persist(componentDetails);
                    }
                }
                catch (Exception e)
                {
                    LOG.error("Exception occurred [{}]", e);
                }
            });

            return true;
        }
        catch (Exception e)
        {
            LOG.error(" Exception occurred while persisting scaleio data", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean saveVCenterComponentDetails(final List<ComponentDetails> componentEndpointDetailsList)
    {
        LOG.info("Persisting vCenter Component, Endpoint and Credential UUID");

        if (componentEndpointDetailsList.isEmpty())
        {
            LOG.error("No Components Found");
            return false;
        }

        try
        {
            componentEndpointDetailsList.stream().filter(Objects::nonNull).forEach(componentDetails -> {
                String componentUuid = componentDetails.getComponentUuid();

                try
                {
                    TypedQuery<ComponentDetails> componentDetailsTypedQuery = entityManager
                            .createQuery("select c from ComponentDetails as c where c.componentUuid =:componentUuid",
                                    ComponentDetails.class);
                    componentDetailsTypedQuery.setParameter("componentUuid", componentUuid);

                    final List<ComponentDetails> existingComponentList = componentDetailsTypedQuery.getResultList();
                    if (existingComponentList != null && !existingComponentList.isEmpty())
                    {
                        entityManager.merge(existingComponentList.get(0));
                        entityManager.flush();
                    }
                    else
                    {
                        entityManager.persist(componentDetails);
                    }
                }
                catch (Exception e)
                {
                    LOG.error("Exception occurred while persisting vCenter component details[{}]", e);
                }
            });

            return true;
        }
        catch (Exception e)
        {
            LOG.error(" Exception occurred while persisting vCenter data", e);
            return false;
        }
    }

    @Override
    public ComponentEndpointIds getComponentEndpointIds(final String componentType)
    {
        return this.getComponentEndpointIds(componentType, null, null);
    }

    @Override
    public ComponentEndpointIds getComponentEndpointIds(final String componentType, final String endpointType, final String credentialName)
    {
        final TypedQuery<ComponentDetails> typedQuery = entityManager
                .createQuery("select ceids from ComponentDetails as ceids where ceids.componentType = :componentType",
                        ComponentDetails.class);
        typedQuery.setParameter("componentType", componentType);

        final List<ComponentDetails> componentDetailsList = typedQuery.getResultList();

        if (!CollectionUtils.isEmpty(componentDetailsList))
        {
            final ComponentDetails componentDetails = componentDetailsList.get(0);

            if (componentDetails == null)
            {
                return null;
            }

            final List<EndpointDetails> endpointDetailsList = componentDetails.getEndpointDetails();

            if (CollectionUtils.isEmpty(endpointDetailsList))
            {
                return null;
            }

            final EndpointDetails endpointDetails = endpointType != null ?
                    endpointDetailsList.stream().filter(Objects::nonNull).filter(ep -> ep.getType().equalsIgnoreCase(endpointType))
                            .findFirst().orElse(null) :
                    endpointDetailsList.get(0);

            if (endpointDetails == null)
            {
                return null;
            }

            final List<CredentialDetails> credentialDetailsList = endpointDetails.getCredentialDetailsList();

            if (CollectionUtils.isEmpty(credentialDetailsList))
            {
                return null;
            }

            final CredentialDetails credentialDetails = credentialName != null ?
                    credentialDetailsList.stream().filter(Objects::nonNull)
                            .filter(c -> c.getCredentialName().equalsIgnoreCase(credentialName)).findFirst().orElse(null) :
                    credentialDetailsList.get(0);

            if (credentialDetails == null)
            {
                return null;
            }

            return new ComponentEndpointIds(componentDetails.getComponentUuid(), endpointDetails.getEndpointUuid(),
                    endpointDetails.getEndpointUrl(), credentialDetails.getCredentialUuid());
        }

        LOG.error("No Component Endpoints found in the database");

        return null;
    }

    @Override
    public ComponentEndpointIds getVCenterComponentEndpointIdsByEndpointType(final String endpointType)
    {
        final TypedQuery<ComponentDetails> typedQuery = entityManager.createQuery(
                "select endpoint.componentDetails from EndpointDetails as endpoint where lower(endpoint.type) = :endpointType and endpoint.componentDetails.componentType = :componentType",
                ComponentDetails.class);
        typedQuery.setParameter("endpointType", endpointType.toLowerCase());
        typedQuery.setParameter("componentType", "VCENTER");

        try
        {
            final ComponentDetails componentDetails = typedQuery.getSingleResult();

            final List<EndpointDetails> endpointDetailsList = componentDetails.getEndpointDetails();

            if (CollectionUtils.isEmpty(endpointDetailsList))
            {
                return null;
            }

            final EndpointDetails endpointDetails = endpointDetailsList.get(0);

            if (endpointDetails == null)
            {
                return null;
            }

            final List<CredentialDetails> credentialDetailsList = endpointDetails.getCredentialDetailsList();

            if (CollectionUtils.isEmpty(credentialDetailsList))
            {
                return null;
            }

            final CredentialDetails credentialDetails = credentialDetailsList.get(0);

            if (credentialDetails == null)
            {
                return null;
            }

            return new ComponentEndpointIds(componentDetails.getComponentUuid(), endpointDetails.getEndpointUuid(),
                    endpointDetails.getEndpointUrl(), credentialDetails.getCredentialUuid());

        }
        catch (Exception e)
        {
            LOG.error("Exception occurred while fetching the component details", e);
            return null;
        }
    }

    @Override
    @Transactional
    public boolean saveNodeInventory(final NodeInventory nodeInventory)
    {
        LOG.info("Persisting Node Inventory data ...");

        if (nodeInventory == null)
        {
            LOG.error("Node inventory data can not be persisted because it empty.");
            return false;
        }

        NodeInventory nodeInventoryData = this.getNodeInventory(nodeInventory.getSymphonyUUID());
        if (nodeInventoryData != null)
        {
            //If it already exists, delete and save the new one.
            entityManager.remove(nodeInventoryData);
        }

        entityManager.persist(nodeInventory);
        entityManager.flush();

        return true;
    }

    @Override
    public NodeInventory getNodeInventory(final String symphonyUUID)
    {
        NodeInventory result = null;

        final TypedQuery<NodeInventory> query;
        try
        {
            query = entityManager
                    .createQuery("SELECT ni FROM NodeInventory as ni where ni.symphonyUUID=:symphonyUUID", NodeInventory.class);

            query.setParameter("symphonyUUID", symphonyUUID);

            NodeInventory nodeInventory = query.getSingleResult();

            if (nodeInventory != null && nodeInventory.getNodeInventory() != null)
            {
                result = nodeInventory;
            }
        }
        catch (NoResultException noResultEx)
        {
            //  Do not do anything as the result will be null indicating that no result found.
            LOG.info("No results found for NodeInventory.");
        }

        return result;
    }

    @Override
    public List<NodeInventory> getNodeInventory() {

        List<NodeInventory> result = null;
        try
        {
            final TypedQuery<NodeInventory> query = entityManager
                    .createQuery("SELECT ni FROM NodeInventory as ni", NodeInventory.class);

            List<NodeInventory> nodeInventoryList = query.getResultList();

            if (!CollectionUtils.isEmpty(nodeInventoryList))
            {
                result = nodeInventoryList;
            }
        }
        catch (NoResultException noResultEx)
        {
            //  Do not do anything as the result will be null indicating that no result found.
            LOG.info("No results found for NodeInventory.");
        }

        return result;
    }

    @Override
    @Transactional
    public boolean saveVCenterData(final String jobId, final VCenter vCenterData)
    {
        final TypedQuery<VCenter> vCenterTypedQuery = entityManager.createQuery("select v from VCenter as v", VCenter.class);

        try
        {
            final List<VCenter> vCenterList = vCenterTypedQuery.getResultList();

            //Don't allow multiple discoveries for now
            if (!CollectionUtils.isEmpty(vCenterList))
            {
                LOG.info("Found vCenter data doing nothing");
                return true;
            }
            else
            {
                final TypedQuery<DneJob> query = entityManager
                        .createQuery("select dne from DneJob as dne where dne.id = :jobId", DneJob.class);

                DneJob dneJob = null;

                try
                {
                    dneJob = query.setParameter("jobId", jobId).getSingleResult();
                }
                catch (NoResultException e)
                {
                    LOG.error("No job found with id: {}", jobId);
                    LOG.error("No result", e.getMessage());
                }

                // Not supporting multiple discoveries for now.

                if (dneJob == null)
                {
                    dneJob = new DneJob(jobId, null, vCenterData);
                    entityManager.persist(dneJob);
                }
                else
                {
                    final TypedQuery<VCenter> vCenterDataTypedQuery = entityManager
                            .createQuery("SELECT vcenter from VCenter as vcenter", VCenter.class);

                    try
                    {
                        final VCenter existingVCenter = vCenterDataTypedQuery.getSingleResult();
                        if (existingVCenter != null)
                        {
                            entityManager.merge(existingVCenter);

                            entityManager.flush();

                            return existingVCenter.getUuid() != null;
                        }
                    }
                    catch (Exception e)
                    {
                        LOG.error("No vCenter data exist");
                    }

                    LOG.info("DneJob != null for vCenter save");
                    vCenterData.setJob(dneJob);
                    dneJob.setVcenter(vCenterData);
                    entityManager.merge(dneJob);
                }
                entityManager.flush();

                return dneJob.getUuid() != null;
            }
        }
        catch (Exception e)
        {
            LOG.error("Exception occurred [{}]", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean saveScaleIoData(final String jobId, final ScaleIOData scaleIOData)
    {
        final TypedQuery<ScaleIOData> scaleIODataTypedQuery = entityManager
                .createQuery("select s from ScaleIOData as s", ScaleIOData.class);

        try
        {
            final List<ScaleIOData> scaleIODataResultList = scaleIODataTypedQuery.getResultList();

            // Multiple discoveries not supported yet
            if (!CollectionUtils.isEmpty(scaleIODataResultList))
            {
                LOG.info("Found ScaleIO Data doing nothing");
                return true;
            }
            else
            {
                final TypedQuery<DneJob> query = entityManager
                        .createQuery("select dne from DneJob as dne where dne.id = :jobId", DneJob.class);

                DneJob dneJob = null;

                try
                {
                    dneJob = query.setParameter("jobId", jobId).getSingleResult();
                }
                catch (NoResultException e)
                {
                    LOG.error("No job found with id: {}", jobId);
                    LOG.error("No result", e.getMessage());
                }

                // Not supporting multiple discoveries for now.

                if (dneJob == null)
                {
                    dneJob = new DneJob(jobId, scaleIOData, null);
                    scaleIOData.setJob(dneJob);
                    entityManager.persist(dneJob);
                }
                else
                {
                    final TypedQuery<ScaleIOData> scaleIoDataTypedQuery = entityManager
                            .createQuery("SELECT scaleio from ScaleIOData as scaleio", ScaleIOData.class);

                    try
                    {
                        final ScaleIOData existingScaleIo = scaleIoDataTypedQuery.getSingleResult();
                        if (existingScaleIo != null)
                        {
                            entityManager.merge(existingScaleIo);

                            entityManager.flush();

                            return existingScaleIo.getUuid() != null;
                        }
                    }
                    catch (Exception e)
                    {
                        LOG.error("No ScaleIOData data exist");
                    }

                    LOG.info("DneJob != null for scaleio save");
                    scaleIOData.setJob(dneJob);
                    dneJob.setScaleIO(scaleIOData);
                    entityManager.merge(dneJob);
                }
                entityManager.flush();

                return dneJob.getUuid() != null;
            }
        }
        catch (Exception e)
        {
            LOG.error("Exception occurred [{}]", e);
            return false;
        }
    }

    @Override
    public Host getVCenterHost(final String hostName)
    {
        final TypedQuery<Host> query = entityManager.createQuery("SELECT h FROM Host as h where h.name=:hostName", Host.class);
        query.setParameter("hostName", hostName);
        return query.getSingleResult();
    }

    @Override
    public List<Host> getVCenterHosts()
    {
        final TypedQuery<Host> query = entityManager.createQuery("SELECT h FROM Host as h", Host.class);
        return query.getResultList();
    }

    @Override
    public List<String> getDnsServers()
    {
        final Set<String> dnsServers = new HashSet<>();

        final List<Host> vCenterHosts = getVCenterHosts();

        List<String> returnVal = new ArrayList<>();
        if (!CollectionUtils.isEmpty(vCenterHosts))
        {
            vCenterHosts.stream().filter(Objects::nonNull)
                .forEach(hs -> hs.getHostDnsConfig().getDnsConfigIPs().stream().filter(Objects::nonNull).forEach(dnsServers::add));
            returnVal.addAll(dnsServers);
        }

        return returnVal;
    }

    @Override
    public Host getExistingVCenterHost()
    {
        final TypedQuery<Host> query = entityManager.createQuery("SELECT h FROM Host as h", Host.class);
        final List<Host> resultList = query.getResultList();

        if (resultList == null || resultList.isEmpty())
        {
            throw new NoResultException("No Host exist");
        }

        return resultList.get(0);
    }

    @Override
    public List<PortGroup> getPortGroups()
    {
        final TypedQuery<PortGroup> query = entityManager.createQuery("SELECT p FROM PortGroup as p", PortGroup.class);
        return query.getResultList();
    }

    @Override
    public ScaleIOData getScaleIoDataByJobId(final String jobId)
    {
        final TypedQuery<ScaleIOData> query = entityManager
                .createQuery("SELECT dneJob.scaleIOData FROM DneJob as dneJob WHERE dneJob.id = :jobId", ScaleIOData.class);
        query.setParameter("jobId", jobId);

        final List<ScaleIOData> scaleIODataList = query.getResultList();

        if (!CollectionUtils.isEmpty(scaleIODataList))
        {
            return scaleIODataList.stream().findFirst().orElseGet(null);
        }

        return null;
    }

    @Override
    public ScaleIOData getScaleIoData()
    {
        final TypedQuery<ScaleIOData> query = entityManager.createQuery("SELECT scaleio FROM ScaleIOData as scaleio", ScaleIOData.class);

        final List<ScaleIOData> scaleIODataList = query.getResultList();

        if (!CollectionUtils.isEmpty(scaleIODataList))
        {
            return scaleIODataList.stream().findFirst().orElseGet(null);
        }

        return null;
    }

    @Override
    public List<PciDevice> getPciDeviceList()
    {
        final TypedQuery<PciDevice> typedQuery = entityManager.createQuery("select p from PciDevice as p", PciDevice.class);
        return typedQuery.getResultList();
    }

    @Override
    public String getClusterId(final String clusterName)
    {
        final TypedQuery<String> typedQuery = entityManager
                .createQuery("select c.id from Cluster as c where c.name = :clusterName", String.class);
        typedQuery.setParameter("clusterName", clusterName);

        try
        {
            return typedQuery.getSingleResult();
        }
        catch (Exception e)
        {
            LOG.error("Exception Occurred [{}]", e);
            return null;
        }
    }

    @Override
    public String getDataCenterName(final String clusterName)
    {
        final TypedQuery<String> typedQuery = entityManager
                .createQuery("select c.dataCenter.name from Cluster as c where c.name = :clusterName", String.class);
        typedQuery.setParameter("clusterName", clusterName);

        try
        {
            return typedQuery.getSingleResult();
        }
        catch (Exception e)
        {
            LOG.error("Exception Occurred [{}]", e);
            return null;
        }
    }

    @Override
    public String getVlanIdVmk0()
    {
        final TypedQuery<String> typedQuery = entityManager.createQuery(
                "select p.vlanId from PortGroup as p " + "join VirtualNicDVPortGroup vnpg on p.id = vnpg.portGroupId "
                        + "join VirtualNic vnic on vnic.uuid = vnpg.virtualNic.uuid " + "where vnic.device = :vNicDevice", String.class);

        typedQuery.setParameter("vNicDevice", "vmk0");

        try
        {
            return typedQuery.getSingleResult();
        }
        catch (Exception e)
        {
            LOG.error("Exception Occurred [{}]", e);
            return null;
        }
    }

    @Override
    public String getDomainName()
    {
        final TypedQuery<HostDnsConfig> hostDnsConfigQuery = entityManager
                .createQuery("select hostDnsConfig from HostDnsConfig as hostDnsConfig", HostDnsConfig.class);

        final List<HostDnsConfig> configs = hostDnsConfigQuery.getResultList();

        if (configs == null)
        {
            return null;
        }

        HostDnsConfig config = configs.stream().filter(Objects::nonNull).findFirst().orElse(null);

        if (config == null)
        {
            return null;
        }

        if (StringUtils.isNotEmpty(config.getDomainName()))
        {
            return config.getDomainName();
        }
        else
        {
            List<String> searchDomains = config.getSearchDomains();

            return searchDomains.stream().filter(StringUtils::isNotEmpty).findFirst().orElse(null);
        }
    }

    @Override
    public Map<String, String> getDvSwitchNames()
    {
        final Map<String, String> switchNames = new HashMap<>();

        try
        {

            final TypedQuery<String> typedQueryDvSwitch0 = entityManager
                    .createQuery("select dvs.name from DVSwitch as dvs where lower(dvs.name) like '%dvswitch0%'", String.class);
            switchNames.put("dvswitch0", typedQueryDvSwitch0.getSingleResult());

            final TypedQuery<String> typedQueryDvSwitch1 = entityManager
                    .createQuery("select dvs.name from DVSwitch as dvs where lower(dvs.name) like '%dvswitch1%'", String.class);
            switchNames.put("dvswitch1", typedQueryDvSwitch1.getSingleResult());

            final TypedQuery<String> typedQueryDvSwitch2 = entityManager
                    .createQuery("select dvs.name from DVSwitch as dvs where lower(dvs.name) like '%dvswitch2%'", String.class);
            switchNames.put("dvswitch2", typedQueryDvSwitch2.getSingleResult());

        }
        catch (Exception e)
        {
            LOG.error("Exception Occurred [{}]", e);
            return null;
        }

        return switchNames;
    }

    @Override
    public Map<String, String> getDvPortGroupNames(final Map<String, String> dvSwitchMap)
    {
        final Map<String, String> dvPortGroupAssociationMap = new HashMap<>();

        try
        {
            final TypedQuery<String> typedQueryVmk0Management = entityManager.createQuery(
                    "select p.name from PortGroup as p " + "join VirtualNicDVPortGroup vnpg on p.id = vnpg.portGroupId "
                            + "join VirtualNic vnic on vnic.uuid = vnpg.virtualNic.uuid " + "where vnic.device = :vNicDevice",
                    String.class);

            typedQueryVmk0Management.setParameter("vNicDevice", "vmk0");
            dvPortGroupAssociationMap.put("esx-mgmt", typedQueryVmk0Management.getSingleResult());

            final TypedQuery<String> typedQueryVmotionManagement = entityManager.createQuery(
                    "select p.name from PortGroup as p join DVSwitch dvs on p.dvSwitch.uuid = dvs.uuid " + "where dvs.name like '%"
                            + dvSwitchMap.get("dvswitch0") + "%' and LOWER(p.name) like '%vmotion%'", String.class);
            dvPortGroupAssociationMap.put("vmotion", typedQueryVmotionManagement.getSingleResult());

            final TypedQuery<String> typedQuerySioData1 = entityManager.createQuery(
                    "select p.name from PortGroup as p join DVSwitch dvs on p.dvSwitch.uuid = dvs.uuid " + "where dvs.name like '%"
                            + dvSwitchMap.get("dvswitch1") + "%' and LOWER(p.name) like '%sio-data1%'", String.class);
            dvPortGroupAssociationMap.put("sio-data1", typedQuerySioData1.getSingleResult());

            final TypedQuery<String> typedQuerySioData2 = entityManager.createQuery(
                    "select p.name from PortGroup as p join DVSwitch dvs on p.dvSwitch.uuid = dvs.uuid " + "where dvs.name like '%"
                            + dvSwitchMap.get("dvswitch2") + "%' and LOWER(p.name) like '%sio-data2%'", String.class);
            dvPortGroupAssociationMap.put("sio-data2", typedQuerySioData2.getSingleResult());

        }
        catch (Exception e)
        {
            LOG.error("Exception Occurred [{}]", e);
            return null;
        }

        return dvPortGroupAssociationMap;
    }

    @Override
    public Map<String, String> getScaleIoNetworkNames(final Map<String, String> switchNames)
    {
        final Map<String, String> switchAndScaleIoNetworkMappings = new HashMap<>();

        final String dvSwitch0 = switchNames.get("dvswitch0");
        final String dvSwitch1 = switchNames.get("dvswitch1");
        final String dvSwitch2 = switchNames.get("dvswitch2");

        try
        {
            final TypedQuery<String> typedQueryScaleIoManagement = entityManager.createQuery(
                    "select p.name from PortGroup as p join DVSwitch dvs on p.dvSwitch.uuid = dvs.uuid " + "where dvs.name like '%"
                            + dvSwitch0 + "%' and LOWER(p.name) like '%sio-mgmt%'", String.class);
            switchAndScaleIoNetworkMappings.put(dvSwitch0, typedQueryScaleIoManagement.getSingleResult());

            final TypedQuery<String> typedQuerySioData1 = entityManager.createQuery(
                    "select p.name from PortGroup as p join DVSwitch dvs on p.dvSwitch.uuid = dvs.uuid " + "where dvs.name like '%"
                            + dvSwitch1 + "%' and LOWER(p.name) like '%sio-data1%'", String.class);
            switchAndScaleIoNetworkMappings.put(dvSwitch1, typedQuerySioData1.getSingleResult());

            final TypedQuery<String> typedQuerySioData2 = entityManager.createQuery(
                    "select p.name from PortGroup as p join DVSwitch dvs on p.dvSwitch.uuid = dvs.uuid " + "where dvs.name like '%"
                            + dvSwitch2 + "%' and LOWER(p.name) like '%sio-data2%'", String.class);
            switchAndScaleIoNetworkMappings.put(dvSwitch2, typedQuerySioData2.getSingleResult());
        }
        catch (Exception e)
        {
            LOG.error("Exception Occurred [{}]", e);
            return null;
        }

        return switchAndScaleIoNetworkMappings;
    }

    @Override
    public List<ScaleIOProtectionDomain> getScaleIoProtectionDomains()
    {
        final TypedQuery<ScaleIOProtectionDomain> query = entityManager
                .createQuery("SELECT scaleio FROM ScaleIOProtectionDomain as scaleio", ScaleIOProtectionDomain.class);

        final List<ScaleIOProtectionDomain> scaleIOProtectionDomainList = query.getResultList();

        if (!CollectionUtils.isEmpty(scaleIOProtectionDomainList))
        {
            return scaleIOProtectionDomainList;
        }

        throw new NoResultException("No protection domain found");
    }

    @Override
    @Transactional
    public boolean saveDiscoveredNodeInfo(DiscoveredNodeInfo discoveredNodeInfo)
    {
        LOG.info("Persisting discovered node info data ...");

        try
        {
            DiscoveredNodeInfo nodeInfo = this.getDiscoveredNodeInfo(discoveredNodeInfo.getSymphonyUuid());
            if (nodeInfo != null)
            {
                //If it already exists, delete and save the new one.
                entityManager.remove(nodeInfo);
            }
        }
        catch (NoResultException noResultEx)
        {
            //  Do not do anything as the result will be null indicating that no result found.
        }

        entityManager.persist(discoveredNodeInfo);
        entityManager.flush();

        return true;
    }

    @Override
    public DiscoveredNodeInfo getDiscoveredNodeInfo(String uuid)
    {
        final TypedQuery<DiscoveredNodeInfo> query = entityManager
                .createQuery("SELECT n FROM DiscoveredNodeInfo as n where n.symphonyUuid=:symphonyUuid", DiscoveredNodeInfo.class);
        query.setParameter("symphonyUuid", uuid);
        return query.getSingleResult();
    }

    @Override
    public List<DiscoveredNodeInfo> getDiscoveredNodeInfo()
    {
        final TypedQuery<DiscoveredNodeInfo> query = entityManager
                .createQuery("SELECT discoveryNodeInfo FROM DiscoveredNodeInfo as discoveryNodeInfo", DiscoveredNodeInfo.class);
        return query.getResultList();
    }

    @Override
    @Transactional
    public ScaleIOProtectionDomain createProtectionDomain(String jobId, String protectionDomainId, String protectionDomainName)
    {
        ScaleIOData scaleIOData = getScaleIoDataByJobId(jobId);

        try
        {
            final ScaleIOProtectionDomain protectionDomain = new ScaleIOProtectionDomain(protectionDomainId, protectionDomainName, "Active");
            scaleIOData.addProtectionDomain(protectionDomain);

            entityManager.merge(protectionDomain);

            entityManager.flush();

            return protectionDomain;

        }
        catch (Exception e)
        {
            LOG.error("Error creating protection domain. ", e);
        }
        return null;
    }

    @Override
    @Transactional
    public ScaleIOStoragePool createStoragePool(String proptectionDomainId, String storagePoolId, String storagePoolName)
    {
        final TypedQuery<ScaleIOProtectionDomain> protectionDomainTypedQuery = entityManager
                .createQuery("SELECT p from ScaleIOProtectionDomain as p where p.id =:proptectionDomainId", ScaleIOProtectionDomain.class);
        protectionDomainTypedQuery.setParameter("proptectionDomainId", proptectionDomainId);

        ScaleIOStoragePool storagePool = new ScaleIOStoragePool(storagePoolId, storagePoolName, -1, -1, 0, false, false, true);
        storagePool.setDevices(Collections.emptyList());

        try
        {
            final ScaleIOProtectionDomain protectionDomain = protectionDomainTypedQuery.getSingleResult();

            storagePool.setProtectionDomain(protectionDomain);

            protectionDomain.addStoragePool(storagePool);

            entityManager.merge(protectionDomain);

            entityManager.flush();
        }
        catch (Exception e)
        {
            LOG.error("Error creating storage pool record. ", e);
        }
        return storagePool;
    }

    @Override
    @Transactional
    public boolean cleanInMemoryDatabase()
    {
        final TypedQuery<DneJob> query = entityManager.createQuery("select dne from DneJob as dne", DneJob.class);

        List<DneJob> dneJobs = query.getResultList();
        if (!CollectionUtils.isEmpty(dneJobs))
        {
            entityManager.remove(dneJobs.get(0));
            entityManager.flush();
        }

        return true;
    }

    @Override
    public List<String> getAllIpAddresses()
    {
        List<String> allIps = new ArrayList<>();

        TypedQuery<String> query = entityManager.createQuery("SELECT s.ip FROM ScaleIOIP as s", String.class);
        List<String> allScaleIOIPs = query.getResultList();
        allIps.addAll(allScaleIOIPs != null ? allScaleIOIPs : Collections.emptyList());

        query = entityManager.createQuery("SELECT s.ip FROM ScaleIORoleIP as s", String.class);
        List<String> allScaleIORoleIPs = query.getResultList();
        allIps.addAll(allScaleIORoleIPs != null ? allScaleIORoleIPs : Collections.emptyList());

        query = entityManager.createQuery("SELECT s.ip FROM VirtualNic as s", String.class);
        List<String> allVirtualNicIps = query.getResultList();
        allIps.addAll(allVirtualNicIps != null ? allVirtualNicIps : Collections.emptyList());

        query = entityManager.createQuery("SELECT s.ipAddress FROM VMIP as s", String.class);
        List<String> allVMIPs = query.getResultList();
        allIps.addAll(allVMIPs != null ? allVMIPs : Collections.emptyList());

        return allIps;
    }
}
