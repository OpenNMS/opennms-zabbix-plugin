package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.Test;
import org.opennms.integration.api.v1.config.datacollection.graphs.PrefabGraph;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Graph;
import org.opennms.plugins.zabbix.model.Template;

public class ZabbixGraphGeneratorTest {

    @Test
    public void canGenerateGraph() {
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        Template template = zabbixTemplateHandler.getTemplateByName("Linux network interfaces by Zabbix agent")
                .orElseThrow(() -> new RuntimeException("Expected template was not found :("));
        DiscoveryRule netIfDiscovery = template.getDiscoveryRuleByName("Network interface discovery")
                .orElseThrow(() -> new RuntimeException("Expected rule was not found :("));
        assertThat(netIfDiscovery.getGraphPrototypes(), hasSize(1));

        ZabbixGraphGenerator zabbixGraphGenerator = new ZabbixGraphGenerator();
        Graph graph = netIfDiscovery.getGraphPrototypes().get(0);
        PrefabGraph prefabGraph = zabbixGraphGenerator.toPrefabGraph(graph, netIfDiscovery);
        assertThat(prefabGraph.getCommand(), equalTo("--title=\"Interface {#IFNAME}: Network traffic\"\n" +
                "DEF:m0={rrd1}:net_if_in:AVERAGE\n" +
                "DEF:m1={rrd1}:net_if_in_dropped:AVERAGE\n" +
                "DEF:m2={rrd1}:net_if_in_errors:AVERAGE\n" +
                "DEF:m3={rrd1}:net_if_out:AVERAGE\n" +
                "DEF:m4={rrd1}:net_if_out_dropped:AVERAGE\n" +
                "DEF:m5={rrd1}:net_if_out_errors:AVERAGE\n" +
                "LINE1:m0#4e9a06:\"net_if_in\"\n" +
                "LINE1:m1#4e9a06:\"net_if_in_dropped\"\n" +
                "LINE1:m2#4e9a06:\"net_if_in_errors\"\n" +
                "LINE1:m3#4e9a06:\"net_if_out\"\n" +
                "LINE1:m4#4e9a06:\"net_if_out_dropped\"\n" +
                "LINE1:m5#4e9a06:\"net_if_out_errors\"\n"));
    }
}
