package org.opennms.plugins.zabbix;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.collectors.ServiceCollector;
import org.opennms.integration.api.v1.collectors.immutables.ImmutableNumericAttribute;
import org.opennms.integration.api.v1.collectors.immutables.ImmutableStringAttribute;
import org.opennms.integration.api.v1.collectors.resource.NodeResource;
import org.opennms.integration.api.v1.collectors.resource.NumericAttribute;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSet;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSetResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableNodeResource;

public class ZabbixAgentCollector implements ServiceCollector {

    @Override
    public CompletableFuture<CollectionSet> collect(CollectionRequest collectionRequest, Map<String, Object> map) {
        final CompletableFuture<CollectionSet> future = new CompletableFuture<>();
        // TODO: Pull port from map
        try (ZabbixAgentClient client = new ZabbixAgentClient(collectionRequest.getAddress(), ZabbixAgentClient.DEFAULT_PORT)) {
            String version = client.retrieveData("agent.version");
            double vmMemoryBytes = Double.parseDouble(client.retrieveData("vm.memory.size"));
            double vmMemoryFreeBytes = Double.parseDouble(client.retrieveData("vm.memory.size[free]"));
            double vmMemoryPercentageAvailable = Double.parseDouble(client.retrieveData("vm.memory.size[pavailable]"));

            ImmutableCollectionSetResource.Builder<NodeResource> builder = ImmutableCollectionSetResource.newBuilder(NodeResource.class)
                    .setResource(ImmutableNodeResource.newBuilder().setNodeId(1).build())
                    .addStringAttribute(ImmutableStringAttribute.newBuilder()
                            .setGroup("zabbix").setName("agent-version").setValue(version).build())
                    .addNumericAttribute(ImmutableNumericAttribute.newBuilder()
                            .setGroup("zabbix").setName("mem-total-bytes").setValue(vmMemoryBytes).setType(NumericAttribute.Type.GAUGE).build())
                    .addNumericAttribute(ImmutableNumericAttribute.newBuilder()
                            .setGroup("zabbix").setName("mem-free-bytes").setValue(vmMemoryFreeBytes).setType(NumericAttribute.Type.GAUGE).build())
                    .addNumericAttribute(ImmutableNumericAttribute.newBuilder()
                            .setGroup("zabbix").setName("mem-perc-avail").setValue(vmMemoryPercentageAvailable).setType(NumericAttribute.Type.GAUGE).build());

            // Add other keys
            ZabbixTemplateHandler templateHandler = new ZabbixTemplateHandler();
            for (String key : templateHandler.getKeys()) {
                String value = client.retrieveData(key);
                builder.addStringAttribute(ImmutableStringAttribute.newBuilder()
                        .setGroup("zabbix").setName(key).setValue(value).build());
            }

            CollectionSet collectionSet = ImmutableCollectionSet.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .setStatus(CollectionSet.Status.SUCCEEDED)
                    .addCollectionSetResource(builder.build())
                    .build();

            future.complete(collectionSet);
        } catch (IOException|NumberFormatException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void initialize() {
        // pass
    }
}
