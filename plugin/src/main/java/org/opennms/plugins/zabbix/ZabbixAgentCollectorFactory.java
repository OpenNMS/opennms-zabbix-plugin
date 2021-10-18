package org.opennms.plugins.zabbix;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.ServiceCollectorFactory;
import org.opennms.integration.api.v1.dao.NodeDao;

public class ZabbixAgentCollectorFactory implements ServiceCollectorFactory<ZabbixAgentCollector> {

    private final NodeDao nodeDao;
    private final TemplateResolver templateResolver;

    public ZabbixAgentCollectorFactory(NodeDao nodeDao, TemplateResolver templateResolver) {
        this.nodeDao = Objects.requireNonNull(nodeDao);
        this.templateResolver = Objects.requireNonNull(templateResolver);
    }

    @Override
    public ZabbixAgentCollector createCollector() {
        return new ZabbixAgentCollector(nodeDao, templateResolver);
    }

    @Override
    public String getCollectorClassName() {
        return ZabbixAgentCollector.class.getCanonicalName();
    }

    @Override
    public Map<String, Object> getRuntimeAttributes(CollectionRequest collectionRequest) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> marshalParameters(Map<String, Object> map) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> unmarshalParameters(Map<String, String> map) {
        return new HashMap<>();
    }
}
