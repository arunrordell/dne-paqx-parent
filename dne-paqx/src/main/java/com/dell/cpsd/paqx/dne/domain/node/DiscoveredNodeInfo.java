/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 */
package com.dell.cpsd.paqx.dne.domain.node;

import com.dell.cpsd.paqx.dne.service.model.NodeStatus;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Model class for node info
 * * <p>
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved.
 * Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */
@Entity
// we save this in h2 database for now. Later RackHD node discovered event should have these value
// we'll update the db in node discovery paqx to store them and use by findAvailable nodes and
// updateSystemDefinition
@Table(name="DISCOVERED_NODE_INFO")
public class DiscoveredNodeInfo {

    @Id
    @Column(name = "SYMPHONY_UUID", unique = true, nullable = false)
    private String symphonyUuid;

    @Column(name = "NODE_STATUS")
    private NodeStatus nodeStatus;

    @Column(name = "SERIAL_NUMBER",unique = true )
    private String serialNumber;

    @Column(name = "MODEL")
    private String model;

    @Column(name = "MODEL_FAMILY")
    private String modelFamily;

    @Column(name = "PRODCUT")
    private String product;

    @Column(name = "PRODCUT_FAMILY")
    private String productFamily;

    @Column(name = "VENDOR")
    private String vendor;

    public DiscoveredNodeInfo() {
    }

    public DiscoveredNodeInfo(String model, String modelFamily, String product, String productFamily, String serialNumber, String symphonyUUID) {
        this.model = model;
        this.modelFamily = modelFamily;
        this.product = product;
        this.productFamily = productFamily;
        this.serialNumber = serialNumber;
        this.symphonyUuid = symphonyUUID;
    }

    public DiscoveredNodeInfo(String model, String modelFamily, String product, String productFamily, String serialNumber, String symphonyUUID, String vendor) {
        this.model = model;
        this.modelFamily = modelFamily;
        this.product = product;
        this.productFamily = productFamily;
        this.serialNumber = serialNumber;
        this.symphonyUuid = symphonyUUID;
        this.vendor  = vendor;
    }

    public String getVendor()
    {
        return vendor;
    }

    public void setVendor(final String vendor)
    {
        this.vendor = vendor;
    }

    public void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public String getModel() {
        return model;
    }

    public String getModelFamily() {
        return modelFamily;
    }

    public NodeStatus getNodeStatus() {
        return nodeStatus;
    }

    public String getProduct() {
        return product;
    }

    public String getProductFamily() {
        return productFamily;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getSymphonyUuid() {
        return symphonyUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiscoveredNodeInfo that = (DiscoveredNodeInfo) o;

        if (!symphonyUuid.equals(that.symphonyUuid)) return false;
        if (nodeStatus != that.nodeStatus) return false;
        if (serialNumber != null ? !serialNumber.equals(that.serialNumber) : that.serialNumber != null) return false;
        if (model != null ? !model.equals(that.model) : that.model != null) return false;
        if (modelFamily != null ? !modelFamily.equals(that.modelFamily) : that.modelFamily != null) return false;
        if (product != null ? !product.equals(that.product) : that.product != null) return false;
        if (vendor != null ? !vendor.equals(that.vendor) : that.vendor != null) return false;
        return productFamily != null ? productFamily.equals(that.productFamily) : that.productFamily == null;

    }

    @Override
    public int hashCode() {
        int result = symphonyUuid.hashCode();
        result = 31 * result + (nodeStatus != null ? nodeStatus.hashCode() : 0);
        result = 31 * result + (serialNumber != null ? serialNumber.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (modelFamily != null ? modelFamily.hashCode() : 0);
        result = 31 * result + (product != null ? product.hashCode() : 0);
        result = 31 * result + (productFamily != null ? productFamily.hashCode() : 0);
        result = 31 * result + (vendor!= null ? vendor.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DiscoveredNodeInfo{" +
                "model='" + model + '\'' +
                ", symphonyUuid='" + symphonyUuid + '\'' +
                ", nodeStatus=" + nodeStatus +
                ", serialNumber='" + serialNumber + '\'' +
                ", modelFamily='" + modelFamily + '\'' +
                ", product='" + product + '\'' +
                ", productFamily='" + productFamily + '\'' +
                ", vendor='" + vendor + '\'' +
                '}';
    }
}
