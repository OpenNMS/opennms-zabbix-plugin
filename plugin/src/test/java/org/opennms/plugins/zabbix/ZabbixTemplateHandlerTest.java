package org.opennms.plugins.zabbix;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.Test;

public class ZabbixTemplateHandlerTest {

    @Test
    public void canGetKeys() {
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        List<String> keys = zabbixTemplateHandler.getKeys();

        // Verify some known keys
        assertThat(keys, hasItem("system.cpu.num"));
        assertThat(keys, hasItem("wmi.get[root/cimv2,\"Select NumberOfLogicalProcessors from Win32_ComputerSystem\"]"));

        System.out.println(keys.size() + " " + keys);
    }
}
