package org.opennms.plugins.zabbix.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.Test;

public class ZabbixKeyTest {

    @Test
    public void canParseKeys() {
        ZabbixKey key = new ZabbixKey("vm.memory.utilization");
        assertThat(key.getName(), equalTo("vm.memory.utilization"));
        assertThat(key.getParameters(), hasSize(0));

        key = new ZabbixKey("net.if.out[\"{#IFNAME}\",errors]");
        assertThat(key.getName(), equalTo("net.if.out"));
        assertThat(key.getParameters(), contains("\"{#IFNAME}\"", "errors"));
    }
}
