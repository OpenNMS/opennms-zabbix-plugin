package org.opennms.plugins.zabbix.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import org.junit.Test;
import org.opennms.plugins.zabbix.ZabbixTemplateHandler;

public class ResourceTypeExtensionTest {

    @Test
    public void canLoad() {
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        ResourceTypesExtension resourceTypesExtension = new ResourceTypesExtension(zabbixTemplateHandler);
        assertThat(resourceTypesExtension.getResourceTypes(), not(empty()));
    }
}
