package org.opennms.plugins.zabbix.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.Test;
import org.opennms.integration.api.v1.config.thresholding.GroupDefinition;
import org.opennms.plugins.zabbix.ZabbixTemplateHandler;

public class ThresholdingConfigExtensionTest {

    @Test
    public void canLoad() {
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        ThresholdingConfigExtension thresholdingConfigExtension = new ThresholdingConfigExtension(zabbixTemplateHandler);
        List<GroupDefinition> groupDefinitions = thresholdingConfigExtension.getGroupDefinitions();
        assertThat(groupDefinitions, not(empty()));
        GroupDefinition groupDefinition = groupDefinitions.get(0);
        // Ensure there are some expression
        assertThat(groupDefinition.getExpressions(), not(empty()));
    }
}
