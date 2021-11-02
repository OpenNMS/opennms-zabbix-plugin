package org.opennms.plugins.zabbix;

import java.util.stream.Collectors;

import org.opennms.integration.api.v1.config.datacollection.graphs.PrefabGraph;
import org.opennms.integration.api.v1.config.datacollection.graphs.immutables.ImmutablePrefabGraph;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Graph;
import org.opennms.plugins.zabbix.model.Item;

public class ZabbixGraphGenerator {
    private final ZabbixMetricMapper zabbixMetricMapper = new ZabbixMetricMapper();
    private final ZabbixResourceTypeGenerator zabbixResourceTypeGenerator = new ZabbixResourceTypeGenerator();

    public PrefabGraph toPrefabGraph(Item item) {
        ImmutablePrefabGraph.Builder builder = ImmutablePrefabGraph.newBuilder();
        builder.setName(ZabbixResourceTypeGenerator.sanitizeResourceName(item.getKey()));
        builder.setTitle(item.getName());
        String metricName = zabbixMetricMapper.getMetricName(item.getKey());
        builder.setColumns(new String[]{metricName});
        // Determine resource type
        String resourceType = "nodeSnmp";
        builder.setTypes(new String[]{resourceType});
        StringBuilder sb = new StringBuilder();
        sb.append("--title=\"");
        sb.append(item.getName());
        sb.append("\"\n");

        sb.append("DEF:");
        sb.append("m0");
        sb.append("={rrd1}:");
        sb.append(metricName);
        sb.append(":AVERAGE\n");

        sb.append("LINE1:");
        sb.append("m0");
        sb.append("#4e9a06");
        sb.append(":\"");
        sb.append(item.getName());
        sb.append("\"\n");

        builder.setCommand(sb.toString());
        return builder.build();
    }

    public PrefabGraph toPrefabGraph(Graph graph, DiscoveryRule rule) {
        ImmutablePrefabGraph.Builder builder = ImmutablePrefabGraph.newBuilder();
        builder.setName(ZabbixResourceTypeGenerator.sanitizeResourceName(graph.getName()));
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
        sb.append("\"\n");

//        sb.append("--vertical-label=\"");
//        sb.append(graph.getName());
//        sb.append("\" \\\n");

        int k = 0;
        for (String column : columnNames) {
            sb.append("DEF:");
            sb.append("m"+k);
            sb.append("={rrd1}:");
            sb.append(column);
            sb.append(":AVERAGE\n");
            k++;
        }

        k = 0;
        for (String column : columnNames) {
            sb.append("LINE1:");
            sb.append("m"+k);
            sb.append("#4e9a06");
            sb.append(":\"");
            sb.append(column);
            sb.append("\"\n");
            k++;
        }
        builder.setCommand(sb.toString());
        return builder.build();
    }

}
