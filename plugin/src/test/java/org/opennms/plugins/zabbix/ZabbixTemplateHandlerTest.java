package org.opennms.plugins.zabbix;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.util.List;

import org.junit.Test;

public class ZabbixTemplateHandlerTest {

    @Test
    public void canGetKeys() {
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        List<String> keys = zabbixTemplateHandler.getKeys();
        assertThat(keys, not(empty()));
        System.out.println(keys.size() + " " + keys);
    }
}
