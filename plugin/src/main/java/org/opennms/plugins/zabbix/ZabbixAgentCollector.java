package org.opennms.plugins.zabbix;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.collectors.ServiceCollector;
import org.opennms.integration.api.v1.collectors.resource.GenericTypeResource;
import org.opennms.integration.api.v1.collectors.resource.NodeResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSet;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSetResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableGenericTypeResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableNodeResource;
import org.opennms.integration.api.v1.config.datacollection.ResourceType;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Item;
import org.opennms.plugins.zabbix.model.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Make collector async (required async client)
 * TODO: Collect multiple keys in parallel, leveraging async behavior
 * TODO: Apply resource filtering during collection, and don't rely on the resource definition
 */
public class ZabbixAgentCollector implements ServiceCollector {

    public static final String PORT_KEY = "port";

    private static final Logger LOG = LoggerFactory.getLogger(ZabbixAgentCollector.class);

    private final ZabbixMetricMapper metricMapper = new ZabbixMetricMapper();

    @Override
    public CompletableFuture<CollectionSet> collect(CollectionRequest collectionRequest, Map<String, Object> map) {
        final CompletableFuture<CollectionSet> future = new CompletableFuture<>();

        int port = ZabbixAgentClient.DEFAULT_PORT;
        if (map.containsKey(PORT_KEY)) {
            try {
                port = Integer.parseInt(map.get(PORT_KEY).toString());
            } catch (NumberFormatException nfe) {
                LOG.error("Invalid port '{}', using default: {}", map.get(PORT_KEY), port);
            }
        }

        int nodeId = Integer.parseInt((String)map.get(ZabbixAgentCollectorFactory.NODE_ID_KEY));
        List<Template> templates = (List<Template>)map.get(ZabbixAgentCollectorFactory.TEMPLATES_KEY);

        try (ZabbixAgentClient client = new ZabbixAgentClient(collectionRequest.getAddress(), port)) {
            NodeResource nodeResource = ImmutableNodeResource.newBuilder().setNodeId(nodeId).build();
            ImmutableCollectionSetResource.Builder<NodeResource> nodeResourceBuilder = ImmutableCollectionSetResource.newBuilder(NodeResource.class)
                    .setResource(nodeResource);
            ImmutableCollectionSet.Builder collectionSetBuilder = ImmutableCollectionSet.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .setStatus(CollectionSet.Status.SUCCEEDED);

            // Process the template
            for (Template template : templates) {
                LOG.debug("Processing template with name: {}", template.getName());
                for (Item item : template.getItems()) {
                    try {
                        String value = client.retrieveData(item.getKey());
                        metricMapper.addValueToResource(item, value, nodeResourceBuilder);
                    } catch (ZabbixNotSupportedException e) {
                        // pass
                    }
                }

                final ZabbixResourceTypeGenerator resourceTypeGenerator = new ZabbixResourceTypeGenerator();
                for (DiscoveryRule rule : template.getDiscoveryRules()) {
                    try {
                        final List<Map<String, Object>> entries = client.discoverData(rule.getKey());
                        // We have some data, let's create a new resource type
                        final ResourceType resourceType = resourceTypeGenerator.getResourceTypeFor(rule);
                        for (Map<String, Object> entry : entries) {
                            // Create a new resource for the entry
                            final ImmutableCollectionSetResource.Builder<GenericTypeResource> resourceBuilder = ImmutableCollectionSetResource.newBuilder(GenericTypeResource.class)
                                    .setResource(ImmutableGenericTypeResource.newBuilder()
                                            .setNodeResource(nodeResource)
                                            .setType(resourceType.getName())
                                            .setInstance(resourceTypeGenerator.getIndex(rule, entry))
                                            .build());

                            // Process the items in the rule
                            boolean didAddAttribute = false;
                            for (Item item : rule.getItemPrototypes()) {
                                final String effectiveKey = ZabbixMacroSupport.evaluateMacro(item.getKey(), entry);
                                try {
                                    String value = client.retrieveData(effectiveKey);
                                    metricMapper.addValueToResource(rule, item, value, resourceBuilder);
                                    didAddAttribute = true;
                                } catch (ZabbixNotSupportedException e) {
                                    // pass
                                }
                            }

                            // Only add the resource if we found 1+ items
                            if (didAddAttribute) {
                                collectionSetBuilder.addCollectionSetResource(resourceBuilder.build());
                            }
                        }
                    } catch (ZabbixNotSupportedException e) {
                        // pass
                    }
                }
            }
            collectionSetBuilder.addCollectionSetResource(nodeResourceBuilder.build());
            future.complete(collectionSetBuilder.build());
        } catch (IOException|NumberFormatException e) {
            future.completeExceptionally(e);
        } catch (Exception e) {
            LOG.error("Error during collection for request: {}", collectionRequest, e);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void initialize() {
        // pass
    }
}
