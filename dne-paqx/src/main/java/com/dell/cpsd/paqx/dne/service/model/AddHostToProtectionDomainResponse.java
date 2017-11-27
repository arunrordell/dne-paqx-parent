/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.service.model;

import com.dell.cpsd.service.engineering.standards.DeviceAssignment;

import java.util.Map;

/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @version 1.0
 * @since 1.0
 */
public class AddHostToProtectionDomainResponse extends TaskResponse
{
    private String status;

    private String protectionDomainName;

    private String protectionDomainId;

    private Map<String, DeviceAssignment> storagePoolDetails;

    public AddHostToProtectionDomainResponse()
    {
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getProtectionDomainName()
    {
        return protectionDomainName;
    }

    public void setProtectionDomainName(String protectionDomainName)
    {
        this.protectionDomainName = protectionDomainName;
    }

    public String getProtectionDomainId()
    {
        return protectionDomainId;
    }

    public void setProtectionDomainId(String protectionDomainId)
    {
        this.protectionDomainId = protectionDomainId;
    }

    public Map<String, DeviceAssignment> getStoragePoolDetails()
    {
        return storagePoolDetails;
    }

    public void setStoragePoolDetails(Map<String, DeviceAssignment> storagePoolDetails)
    {
        this.storagePoolDetails = storagePoolDetails;
    }

}
