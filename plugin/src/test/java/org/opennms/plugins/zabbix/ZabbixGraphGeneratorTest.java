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
        assertThat(prefabGraph.getCommand(), equalTo("--title=\"Interface {#IFNAME}: Network traffic\" \\\n" +
                "DEF:m0={rrd1}:net.if.in:AVERAGE \\\nDEF:m1={rrd1}:net.if.in.dropped:AVERAGE \\\n" +
                "DEF:m2={rrd1}:net.if.in.errors:AVERAGE \\\nDEF:m3={rrd1}:net.if.out:AVERAGE \\\n" +
                "DEF:m4={rrd1}:net.if.out.dropped:AVERAGE \\\nDEF:m5={rrd1}:net.if.out.errors:AVERAGE \\\n" +
                "LINE1:m0#4e9a06:\"net.if.in\" \\ \nLINE1:m1#4e9a06:\"net.if.in.dropped\" \\ \n" +
                "LINE1:m2#4e9a06:\"net.if.in.errors\" \\ \nLINE1:m3#4e9a06:\"net.if.out\" \\ \n" +
                "LINE1:m4#4e9a06:\"net.if.out.dropped\" \\ \nLINE1:m5#4e9a06:\"net.if.out.errors\" \\ \n"));
    }
}
