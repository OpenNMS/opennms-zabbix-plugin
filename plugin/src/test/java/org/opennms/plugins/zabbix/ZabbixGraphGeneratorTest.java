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
        assertThat(prefabGraph.getCommand(), equalTo("--title=\"Zabbix: Interface {#IFNAME}: Network traffic\" " +
                "DEF:m0={rrd1}:net_if_in:AVERAGE " +
                "DEF:m1={rrd1}:net_if_in_dropped:AVERAGE " +
                "DEF:m2={rrd1}:net_if_in_errors:AVERAGE " +
                "DEF:m3={rrd1}:net_if_out:AVERAGE " +
                "DEF:m4={rrd1}:net_if_out_dropped:AVERAGE " +
                "DEF:m5={rrd1}:net_if_out_errors:AVERAGE " +
                "LINE1:m0#c2291b:\"net_if_in\" " +
                "LINE1:m1#c2291b:\"net_if_in_dropped\" " +
                "LINE1:m2#c2291b:\"net_if_in_errors\" " +
                "LINE1:m3#c2291b:\"net_if_out\" " +
                "LINE1:m4#c2291b:\"net_if_out_dropped\" " +
                "LINE1:m5#c2291b:\"net_if_out_errors\" "));
    }
}
