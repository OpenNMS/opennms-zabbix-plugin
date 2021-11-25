package org.opennms.plugins.zabbix.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.opennms.integration.api.v1.config.events.EventDefinition;
import org.opennms.integration.api.xml.ClasspathEventDefinitionLoader;
import org.opennms.plugins.zabbix.ZabbixEventGenerator;
import org.opennms.plugins.zabbix.ZabbixTemplateHandler;

public class EventConfExtension implements org.opennms.integration.api.v1.config.events.EventConfExtension {

    private final ZabbixTemplateHandler zabbixTemplateHandler;
    private final ZabbixEventGenerator zabbixEventGenerator = new ZabbixEventGenerator();
    private final ClasspathEventDefinitionLoader classpathEventDefinitionLoader = new ClasspathEventDefinitionLoader(
            EventConfExtension.class,
            "plugin.ext.events.xml"
    );

    public EventConfExtension(ZabbixTemplateHandler zabbixTemplateHandler) {
        this.zabbixTemplateHandler = Objects.requireNonNull(zabbixTemplateHandler);
    }

    @Override
    public List<EventDefinition> getEventDefinitions() {
        final List<EventDefinition> events = new LinkedList<>(classpathEventDefinitionLoader.getEventDefinitions());
        zabbixTemplateHandler.getTemplates().stream()
                .flatMap(t -> t.getItems().stream())
                .flatMap(r -> r.getTriggers().stream())
                .flatMap(t -> zabbixEventGenerator.getEventDefinitions(t).stream())
                .forEach(events::add);
        zabbixTemplateHandler.getTemplates().stream()
                .flatMap(t -> t.getDiscoveryRules().stream())
                .flatMap(r -> r.getTriggerPrototypes().stream())
                .flatMap(t -> zabbixEventGenerator.getEventDefinitions(t).stream())
                .forEach(events::add);
        return events;
    }
}
