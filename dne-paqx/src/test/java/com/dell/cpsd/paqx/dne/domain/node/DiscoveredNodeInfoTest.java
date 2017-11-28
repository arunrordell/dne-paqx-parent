package com.dell.cpsd.paqx.dne.domain.node;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscoveredNodeInfoTest
{
    @Test
    public void testEquals()
    {
        DiscoveredNodeInfo info1 = new DiscoveredNodeInfo("model1", "modelFamily1", "product1", "productFamily1", "serialNumber1", "uuid1","vendor");
        DiscoveredNodeInfo info2 = new DiscoveredNodeInfo("model1", "modelFamily1", "product1", "productFamily1", "serialNumber1", "uuid1","vendor");

        assertTrue(info1.equals(info2));

        Set<DiscoveredNodeInfo> set = new HashSet<>();
        assertTrue(set.add(info1));
        assertFalse(set.add(info2));
    }
}
