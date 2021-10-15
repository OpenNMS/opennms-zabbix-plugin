package org.opennms.plugins.zabbix;

import java.util.HashMap;
import java.util.Map;

import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.ServiceCollectorFactory;

public class ZabbixAgentCollectorFactory implements ServiceCollectorFactory<ZabbixAgentCollector> {
    @Override
    public ZabbixAgentCollector createCollector() {
        return new ZabbixAgentCollector();
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
