package org.opennms.plugins.zabbix;

import java.util.stream.Collectors;

import org.opennms.integration.api.v1.config.datacollection.graphs.PrefabGraph;
import org.opennms.integration.api.v1.config.datacollection.graphs.immutables.ImmutablePrefabGraph;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Graph;

public class ZabbixGraphGenerator {

    public PrefabGraph toPrefabGraph(Graph graph, DiscoveryRule rule) {
        ZabbixMetricMapper zabbixMetricMapper = new ZabbixMetricMapper();
        ZabbixResourceTypeGenerator zabbixResourceTypeGenerator = new ZabbixResourceTypeGenerator();
        ImmutablePrefabGraph.Builder builder = ImmutablePrefabGraph.newBuilder();
        builder.setName(graph.getName());
        builder.setTitle(graph.getName());
        // Determine the metric names for all of the keys
        final String[] columnNames = graph.getGraphItems().stream()
                .map(g -> g.getItem().getKey())
                .map(zabbixMetricMapper::getMetricName)
                .sorted()
                .collect(Collectors.toList()).toArray(new String[]{});
        builder.setColumns(columnNames);
        // Determine resource type
        String resourceType ="nodeSnmp";
        if (rule != null) {
            resourceType = zabbixResourceTypeGenerator.getResourceTypeFor(rule).getName();
        }
        builder.setTypes(new String[]{resourceType});
        StringBuilder sb = new StringBuilder();
        sb.append("--title=\"");
        sb.append(graph.getName());
        sb.append("\" \\\n");

//        sb.append("--vertical-label=\"");
//        sb.append(graph.getName());
//        sb.append("\" \\\n");

        int k = 0;
        for (String column : columnNames) {
            sb.append("DEF:");
            sb.append("m"+k);
            sb.append("={rrd1}:");
            sb.append(column);
            sb.append(":AVERAGE \\\n");
            k++;
        }

        k = 0;
        for (String column : columnNames) {
            sb.append("LINE1:");
            sb.append("m"+k);
            sb.append("#4e9a06");
            sb.append(":\"");
            sb.append(column);
            sb.append("\" \\ \n");
            k++;
        }
        builder.setCommand(sb.toString());
        return builder.build();
    }
}
