package org.opennms.plugins.zabbix;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.ServiceCollectorFactory;
import org.osgi.framework.BundleContext;

public class ZabbixAgentCollectorFactory implements ServiceCollectorFactory<ZabbixAgentCollector> {

    private final BundleContext bundleContext;

    public ZabbixAgentCollectorFactory(BundleContext bundleContext) {
        this.bundleContext = Objects.requireNonNull(bundleContext);
    }

    @Override
    public ZabbixAgentCollector createCollector() {
        return new ZabbixAgentCollector(bundleContext);
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
