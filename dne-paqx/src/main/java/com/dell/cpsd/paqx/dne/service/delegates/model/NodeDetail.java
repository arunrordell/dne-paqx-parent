/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */
package com.dell.cpsd.paqx.dne.service.delegates.model;

import com.dell.cpsd.service.engineering.standards.DeviceAssignment;
import net.minidev.json.annotate.JsonIgnore;

import java.io.Serializable;
import java.util.Map;

public class NodeDetail implements Serializable
{
    private String id;
    private String serviceTag;

    private String idracIpAddress;
    private String idracGatewayIpAddress;
    private String idracSubnetMask;

    private String esxiManagementIpAddress;
    private String esxiManagementGatewayIpAddress;
    private String esxiManagementSubnetMask;
    private String esxiManagementHostname;

    private String scaleIoData1SvmIpAddress;
    private String scaleIoData1SvmSubnetMask;
    private String scaleIoData2SvmIpAddress;
    private String scaleIoData2SvmSubnetMask;
    private String scaleIoData1EsxIpAddress;
    private String scaleIoData1EsxSubnetMask;
    private String scaleIoData2EsxIpAddress;
    private String scaleIoData2EsxSubnetMask;
    private String scaleIoSvmManagementIpAddress;
    private String scaleIoSvmManagementGatewayAddress;
    private String scaleIoSvmManagementSubnetMask;
    private String clusterName;
    private String vMotionManagementIpAddress;
    private String vMotionManagementSubnetMask;
    private String protectionDomainName;
    private String protectionDomainId;

    @JsonIgnore
    private boolean inventoryFailed;

    @JsonIgnore
    private boolean completed;

    public NodeDetail()
    {
    }

    public NodeDetail(final String id, final String serviceTag)
    {
        this.id = id;
        this.serviceTag = serviceTag;
    }

    private Map<String, DeviceAssignment> deviceToDeviceStoragePool;

    public String getIdracIpAddress()
    {
        return idracIpAddress;
    }

    public void setIdracIpAddress(final String idracIpAddress)
    {
        this.idracIpAddress = idracIpAddress;
    }

    public String getIdracGatewayIpAddress()
    {
        return idracGatewayIpAddress;
    }

    public void setIdracGatewayIpAddress(final String idracGatewayIpAddress)
    {
        this.idracGatewayIpAddress = idracGatewayIpAddress;
    }

    public String getIdracSubnetMask()
    {
        return idracSubnetMask;
    }

    public void setIdracSubnetMask(final String idracSubnetMask)
    {
        this.idracSubnetMask = idracSubnetMask;
    }

    public String getEsxiManagementIpAddress()
    {
        return esxiManagementIpAddress;
    }

    public void setEsxiManagementIpAddress(final String esxiManagementIpAddress)
    {
        this.esxiManagementIpAddress = esxiManagementIpAddress;
    }

    public String getEsxiManagementGatewayIpAddress()
    {
        return esxiManagementGatewayIpAddress;
    }

    public void setEsxiManagementGatewayIpAddress(final String esxiManagementGatewayIpAddress)
    {
        this.esxiManagementGatewayIpAddress = esxiManagementGatewayIpAddress;
    }

    public String getEsxiManagementSubnetMask()
    {
        return esxiManagementSubnetMask;
    }

    public void setEsxiManagementSubnetMask(final String esxiManagementSubnetMask)
    {
        this.esxiManagementSubnetMask = esxiManagementSubnetMask;
    }

    public String getEsxiManagementHostname()
    {
        return esxiManagementHostname;
    }

    public void setEsxiManagementHostname(final String esxiManagementHostname)
    {
        this.esxiManagementHostname = esxiManagementHostname;
    }

    public String getScaleIoData1SvmIpAddress()
    {
        return scaleIoData1SvmIpAddress;
    }

    public void setScaleIoData1SvmIpAddress(final String scaleIoData1SvmIpAddress)
    {
        this.scaleIoData1SvmIpAddress = scaleIoData1SvmIpAddress;
    }

    public String getScaleIoData1SvmSubnetMask()
    {
        return scaleIoData1SvmSubnetMask;
    }

    public void setScaleIoData1SvmSubnetMask(final String scaleIoData1SvmSubnetMask)
    {
        this.scaleIoData1SvmSubnetMask = scaleIoData1SvmSubnetMask;
    }

    public String getScaleIoData2SvmIpAddress()
    {
        return scaleIoData2SvmIpAddress;
    }

    public void setScaleIoData2SvmIpAddress(final String scaleIoData2SvmIpAddress)
    {
        this.scaleIoData2SvmIpAddress = scaleIoData2SvmIpAddress;
    }

    public String getvMotionManagementIpAddress()
    {
        return vMotionManagementIpAddress;
    }

    public void setvMotionManagementIpAddress(final String vMotionManagementIpAddress)
    {
        this.vMotionManagementIpAddress = vMotionManagementIpAddress;
    }

    public String getvMotionManagementSubnetMask()
    {
        return vMotionManagementSubnetMask;
    }

    public void setvMotionManagementSubnetMask(final String vMotionManagementSubnetMask)
    {
        this.vMotionManagementSubnetMask = vMotionManagementSubnetMask;
    }

    public String getScaleIoData2SvmSubnetMask()
    {
        return scaleIoData2SvmSubnetMask;
    }

    public String getScaleIoData1EsxIpAddress()
    {
        return scaleIoData1EsxIpAddress;
    }

    public String getScaleIoData1EsxSubnetMask()
    {
        return scaleIoData1EsxSubnetMask;
    }

    public String getScaleIoData2EsxIpAddress()
    {
        return scaleIoData2EsxIpAddress;
    }

    public String getScaleIoData2EsxSubnetMask()
    {
        return scaleIoData2EsxSubnetMask;
    }

    public String getProtectionDomainName()
    {
        return protectionDomainName;
    }

    public String getProtectionDomainId()
    {
        return protectionDomainId;
    }

    public Map<String, DeviceAssignment> getDeviceToDeviceStoragePool()
    {
        return deviceToDeviceStoragePool;
    }

    public void setScaleIoData2SvmSubnetMask(final String scaleIoData2SvmSubnetMask)
    {
        this.scaleIoData2SvmSubnetMask = scaleIoData2SvmSubnetMask;
    }

    public void setScaleIoData1EsxIpAddress(final String scaleIoData1EsxIpAddress)
    {
        this.scaleIoData1EsxIpAddress = scaleIoData1EsxIpAddress;
    }

    public void setScaleIoData1EsxSubnetMask(final String scaleIoData1EsxSubnetMask)
    {
        this.scaleIoData1EsxSubnetMask = scaleIoData1EsxSubnetMask;
    }

    public void setScaleIoData2EsxIpAddress(final String scaleIoData2EsxIpAddress)
    {
        this.scaleIoData2EsxIpAddress = scaleIoData2EsxIpAddress;
    }

    public void setScaleIoData2EsxSubnetMask(final String scaleIoData2EsxSubnetMask)
    {
        this.scaleIoData2EsxSubnetMask = scaleIoData2EsxSubnetMask;
    }

    public void setProtectionDomainName(final String protectionDomainName)
    {
        this.protectionDomainName = protectionDomainName;
    }

    public void setProtectionDomainId(final String protectionDomainId)
    {
        this.protectionDomainId = protectionDomainId;
    }

    public void setDeviceToDeviceStoragePool(final Map<String, DeviceAssignment> deviceToDeviceStoragePool)
    {
        this.deviceToDeviceStoragePool = deviceToDeviceStoragePool;
    }

    public String getId()
    {
        return id;
    }

    public void setId(final String id)
    {
        this.id = id;
    }

    public String getServiceTag()
    {
        return serviceTag;
    }

    public void setServiceTag(final String serviceTag)
    {
        this.serviceTag = serviceTag;
    }

    public String getScaleIoSvmManagementIpAddress()
    {
        return scaleIoSvmManagementIpAddress;
    }

    public void setScaleIoSvmManagementIpAddress(final String scaleIoSvmManagementIpAddress)
    {
        this.scaleIoSvmManagementIpAddress = scaleIoSvmManagementIpAddress;
    }

    public String getScaleIoSvmManagementGatewayAddress()
    {
        return scaleIoSvmManagementGatewayAddress;
    }

    public void setScaleIoSvmManagementGatewayAddress(final String scaleIoSvmManagementGatewayAddress)
    {
        this.scaleIoSvmManagementGatewayAddress = scaleIoSvmManagementGatewayAddress;
    }

    public String getScaleIoSvmManagementSubnetMask()
    {
        return scaleIoSvmManagementSubnetMask;
    }

    public void setScaleIoSvmManagementSubnetMask(final String scaleIoSvmManagementSubnetMask)
    {
        this.scaleIoSvmManagementSubnetMask = scaleIoSvmManagementSubnetMask;
    }

    public String getClusterName()
    {
        return clusterName;
    }

    public void setClusterName(final String clusterName)
    {
        this.clusterName = clusterName;
    }

    public boolean isInventoryFailed()
    {
        return inventoryFailed;
    }

    public void setInventoryFailed(final boolean inventoryFailed)
    {
        this.inventoryFailed = inventoryFailed;
    }

    public boolean isCompleted()
    {
        return completed;
    }

    public void setCompleted(final boolean completed)
    {
        this.completed = completed;
    }
}
