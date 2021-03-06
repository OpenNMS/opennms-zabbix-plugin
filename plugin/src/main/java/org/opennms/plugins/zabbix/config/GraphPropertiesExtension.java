package org.opennms.plugins.zabbix.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.opennms.integration.api.v1.config.datacollection.graphs.PrefabGraph;
import org.opennms.plugins.zabbix.ZabbixGraphGenerator;
import org.opennms.plugins.zabbix.ZabbixTemplateHandler;
import org.opennms.plugins.zabbix.model.Graph;

public class GraphPropertiesExtension implements org.opennms.integration.api.v1.config.datacollection.graphs.GraphPropertiesExtension {

    private final ZabbixTemplateHandler zabbixTemplateHandler;
    private final ZabbixGraphGenerator zabbixGraphGenerator = new ZabbixGraphGenerator();

    public GraphPropertiesExtension(ZabbixTemplateHandler zabbixTemplateHandler) {
        this.zabbixTemplateHandler = Objects.requireNonNull(zabbixTemplateHandler);
    }

    @Override
    public List<PrefabGraph> getPrefabGraphs() {
        final List<PrefabGraph> allGraphs = new LinkedList<>();
        zabbixTemplateHandler.getTemplates()
                .stream().flatMap(t -> t.getItems().stream())
                .map(zabbixGraphGenerator::toPrefabGraph)
                .forEach(allGraphs::add);

        zabbixTemplateHandler.getTemplates()
                .stream().flatMap(t -> t.getDiscoveryRules().stream())
                .flatMap(r -> {
                    List<PrefabGraph> graphs = new LinkedList<>();
                    for (Graph graph : r.getGraphPrototypes()) {
                        graphs.add(zabbixGraphGenerator.toPrefabGraph(graph, r));
                    }
                    return graphs.stream();
                })
                .forEach(allGraphs::add);
        return allGraphs;
    }
}
