/**
 * <p>
 * Copyright &copy; 2017 Dell Inc. or its subsidiaries. All Rights Reserved. Dell EMC Confidential/Proprietary Information
 * </p>
 */

package com.dell.cpsd.paqx.dne.log;

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DneMessageCodeTest
{
    private int    testErrorCode   = 1001;
    private String testMessageCode = "DNEP1001W";

    @Test
    public void getMessageCode() throws Exception
    {
        assertEquals(testMessageCode, DneMessageCode.YYY_W.getMessageCode());
    }

    @Test
    public void getErrorCode() throws Exception
    {
        assertEquals(testErrorCode, DneMessageCode.YYY_W.getErrorCode());
    }

    @Test
    public void getErrorText() throws Exception
    {
        assertThat(DneMessageCode.YYY_W.getErrorText(), containsString(testMessageCode));
    }

    @Test
    public void getMessageTextWithParams() throws Exception
    {
        String parameter = "param1";
        String messageText = DneMessageCode.YYY_W.getMessageText(new Object[] {parameter});

        assertThat(messageText, containsString(testMessageCode));
        assertThat(messageText, containsString(parameter));
    }

    @Test
    public void getMessageTextWithOutParams() throws Exception
    {
        assertThat(DneMessageCode.YYY_W.getMessageText(null), containsString(testMessageCode));
        assertThat(DneMessageCode.YYY_W.getMessageText(new Object[] {}), containsString(testMessageCode));
    }

}