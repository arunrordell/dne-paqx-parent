/**
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries.  All Rights Reserved. Dell EMC Confidential/Proprietary Information
 */

package com.dell.cpsd.paqx.dne.service.amqp.adapter;

import com.dell.cpsd.service.common.client.callback.ServiceResponse;
import com.dell.cpsd.service.common.client.rpc.ServiceCallbackAdapter;
import com.dell.cpsd.virtualization.capabilities.api.HostPowerOperationResponseMessage;
import com.dell.cpsd.virtualization.capabilities.api.MessageProperties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * The tests for RebootHostResponseAdapter class.
 *
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 *
 * @since 1.0
 */
public class RebootHostResponseAdapterTest extends BaseAsynchronousResponseAdapterTest<HostPowerOperationResponseMessage>
{
    @Override
    protected ServiceCallbackAdapter<HostPowerOperationResponseMessage, ServiceResponse<HostPowerOperationResponseMessage>> createTestable()
    {
        return new RebootHostResponseAdapter(this.registry, this.runtimeService);
    }

    @Override
    protected HostPowerOperationResponseMessage createResponseMessageSpy()
    {
        HostPowerOperationResponseMessage theSpy = spy(HostPowerOperationResponseMessage.class);
        theSpy.setMessageProperties(mock(MessageProperties.class));
        return theSpy;
    }
}