package org.opennms.plugins.zabbix.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import org.junit.Ignore;
import org.junit.Test;
import org.opennms.plugins.zabbix.ZabbixTemplateHandler;

public class GraphPropertiesExtensionTest {

    @Test
    @Ignore
    public void canLoad() {
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        GraphPropertiesExtension graphPropertiesExtension = new GraphPropertiesExtension(zabbixTemplateHandler);
        assertThat(graphPropertiesExtension.getPrefabGraphs(), not(empty()));
    }

}
