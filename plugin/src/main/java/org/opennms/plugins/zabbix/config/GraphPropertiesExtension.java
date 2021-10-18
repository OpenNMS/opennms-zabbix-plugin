package org.opennms.plugins.zabbix.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.opennms.integration.api.v1.config.datacollection.graphs.PrefabGraph;
import org.opennms.plugins.zabbix.ZabbixTemplateHandler;

public class GraphPropertiesExtension implements org.opennms.integration.api.v1.config.datacollection.graphs.GraphPropertiesExtension {

    private final ZabbixTemplateHandler zabbixTemplateHandler;

    public GraphPropertiesExtension(ZabbixTemplateHandler zabbixTemplateHandler) {
        this.zabbixTemplateHandler = Objects.requireNonNull(zabbixTemplateHandler);
    }

    @Override
    public List<PrefabGraph> getPrefabGraphs() {
        return Collections.emptyList();
    }
}
