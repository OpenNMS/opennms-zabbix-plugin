package org.opennms.plugins.zabbix.config;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.opennms.integration.api.v1.config.datacollection.ResourceType;
import org.opennms.plugins.zabbix.ZabbixResourceTypeGenerator;
import org.opennms.plugins.zabbix.ZabbixTemplateHandler;

public class ResourceTypesExtension implements org.opennms.integration.api.v1.config.datacollection.ResourceTypesExtension {

    private final ZabbixTemplateHandler zabbixTemplateHandler;

    private final ZabbixResourceTypeGenerator zabbixResourceTypeGenerator = new ZabbixResourceTypeGenerator();

    public ResourceTypesExtension(ZabbixTemplateHandler zabbixTemplateHandler) {
        this.zabbixTemplateHandler = Objects.requireNonNull(zabbixTemplateHandler);
    }

    @Override
    public List<ResourceType> getResourceTypes() {
        return zabbixTemplateHandler.getTemplates()
                .stream().flatMap(t -> t.getDiscoveryRules().stream())
                .map(zabbixResourceTypeGenerator::getResourceTypeFor)
                .collect(Collectors.toList());
    }
}
