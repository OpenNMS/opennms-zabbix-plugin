package org.opennms.plugins.zabbix;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.opennms.integration.api.v1.collectors.resource.NumericAttribute;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Item;
import org.opennms.plugins.zabbix.model.Template;

public class ZabbixMetricMapperTest {

    private ZabbixMetricMapper metricMapper = new ZabbixMetricMapper();

    /**
     *  Item prototype: net.if.out["{#IFNAME}",errors]
     *  Graphs refer key: net.if.out["{#IFNAME}",errors]
     *  Thresholds refer to key: net.if.out["{#IFNAME}",errors]
     */
    @Test
    public void canMapMetrics() {
        assertThat(metricMapper.getMetricName("net.if.out[\"{#IFNAME}\",errors]"), equalTo("net.if.out.errors"));
        assertThat(metricMapper.getMetricName("net.if.in[\"{#IFNAME}\"]"), equalTo("net.if.in"));
        assertThat(metricMapper.getMetricName("vm.memory.utilization"), equalTo("vm.memory.utilization"));
    }

    @Test
    public void canDetermineTypeForMetric() {
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        Template template = zabbixTemplateHandler.getTemplateByName("Linux network interfaces by Zabbix agent")
                .orElseThrow(() -> new RuntimeException("Expected template was not found :("));
        DiscoveryRule netIfDiscovery = template.getDiscoveryRuleByName("Network interface discovery")
                .orElseThrow(() -> new RuntimeException("Expected rule was not found :("));
        Item netIfInBits = netIfDiscovery.getItemPrototypes().stream()
                .filter(item -> item.getKey().equals("net.if.in[\"{#IFNAME}\"]"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Expected item prototype was not found :("));
        assertThat(metricMapper.getMetricType(netIfInBits), equalTo(NumericAttribute.Type.COUNTER));
    }
}
