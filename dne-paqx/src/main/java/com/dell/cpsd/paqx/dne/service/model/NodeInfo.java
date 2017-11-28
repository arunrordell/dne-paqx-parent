/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.model;

/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
*/
public class NodeInfo
{
    private String       symphonyUuid;
    private NodeStatus   nodeStatus;
    private String       serialNumber;
    private String       product;
    private String       vendor;

    public NodeInfo(String symphonyUuid, NodeStatus nodeStatus, String serialNumber, String product, String vendor)
    {
        this.nodeStatus = nodeStatus;
        this.symphonyUuid = symphonyUuid;
        this.serialNumber = serialNumber;
        this.product = product;
        this.vendor = vendor;
    }
    public NodeInfo(String symphonyUuid, NodeStatus nodeStatus)
    {
        this.nodeStatus = nodeStatus;
        this.symphonyUuid = symphonyUuid;
        this.serialNumber = "";
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getSymphonyUuid()
    {
        return symphonyUuid;
    }

    public NodeStatus getNodeStatus()
    {
        return nodeStatus;
    }

    public String getProduct()
    {
        return product;
    }

    public String getVendor()
    {
        return vendor;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "nodeStatus=" + nodeStatus +
                ", symphonyUuid='" + symphonyUuid + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", product='" + product + '\'' +
                ", vendor='" + vendor + '\'' +
                '}';
    }
}
