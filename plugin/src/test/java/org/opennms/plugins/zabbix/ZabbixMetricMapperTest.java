package org.opennms.plugins.zabbix;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.opennms.integration.api.v1.collectors.resource.NumericAttribute;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Item;
import org.opennms.plugins.zabbix.model.Template;

public class ZabbixMetricMapperTest {

    private final ZabbixMetricMapper metricMapper = new ZabbixMetricMapper();

    /**
     *  Item prototype: net.if.out["{#IFNAME}",errors]
     *  Graphs refer key: net.if.out["{#IFNAME}",errors]
     *  Thresholds refer to key: net.if.out["{#IFNAME}",errors]
     */
    @Test
    public void canMapMetrics() {
        assertThat(metricMapper.getMetricName("net.if.out[\"{#IFNAME}\",errors]"), equalTo("net_if_out_errors"));
        assertThat(metricMapper.getMetricName("net.if.in[\"{#IFNAME}\"]"), equalTo("net_if_in"));
        assertThat(metricMapper.getMetricName("vm.memory.utilization"), equalTo("vm_memory_utilization"));
        assertThat(metricMapper.getMetricName("system.cpu.util[,iowait]"), equalTo("system_cpu_util_iowait"));
        assertThat(metricMapper.getMetricName("proc.num[,,run]"), equalTo("proc_num_run"));
        assertThat(metricMapper.getMetricName("vfs.file.contents[/sys/block/{#DEVNAME}/stat]"), equalTo("vfs_file_contents"));
        assertThat(metricMapper.getMetricName("perf_counter_en[\"\\Memory\\Free System Page Table Entries\"]"), equalTo("perf_counter_en_memory_free_system_page_table_entries"));
        assertThat(metricMapper.getMetricName("perf_counter_en[\"\\Processor Information(_total)\\% Interrupt Time\"]"), equalTo("perf_counter_en_processor_information_total_interrupt_time"));
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
