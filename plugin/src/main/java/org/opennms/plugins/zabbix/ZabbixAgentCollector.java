package org.opennms.plugins.zabbix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    private ZabbixAgentClientFactory clientFactory;

    public ZabbixAgentCollector(ZabbixAgentClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public CompletableFuture<CollectionSet> collect(CollectionRequest collectionRequest, Map<String, Object> map) {
        CompletableFuture<CollectionSet> future = new CompletableFuture<>();
        int port = ZabbixAgentClient.DEFAULT_PORT;
        if (map.containsKey(PORT_KEY)) {
            try {
                port = Integer.parseInt(map.get(PORT_KEY).toString());
            } catch (NumberFormatException e) {
                LOG.error("Invalid port '{}', using default: {}", map.get(PORT_KEY), port);
            }
        }
        int nodeId = Integer.parseInt((String) map.get(ZabbixAgentCollectorFactory.NODE_ID_KEY));
        List<Template> templates = (List<Template>) map.get(ZabbixAgentCollectorFactory.TEMPLATES_KEY);
        try {
            ZabbixAgentClient client = clientFactory.createClient(collectionRequest.getAddress(), port);
            NodeResource nodeResource = ImmutableNodeResource.newBuilder().setNodeId(nodeId).build();
            ImmutableCollectionSetResource.Builder<NodeResource> nodeResourceBuilder = ImmutableCollectionSetResource.newBuilder(NodeResource.class)
                    .setResource(nodeResource);
            ImmutableCollectionSet.Builder collectionSetBuilder = ImmutableCollectionSet.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .setStatus(CollectionSet.Status.SUCCEEDED);
            // Process the template
            List<CompletableFuture<Void>> topList = new ArrayList<>();
            for (Template template : templates) {
                List<CompletableFuture<Void>> templateFutures = new ArrayList<>();
                LOG.debug("Processing template with name: {}", template.getName());
                templateFutures.add(processItems(client, null, template.getItems(), nodeResourceBuilder));
                final ZabbixResourceTypeGenerator resourceTypeGenerator = new ZabbixResourceTypeGenerator();
                for (DiscoveryRule rule : template.getDiscoveryRules()) {
                    try {
                        templateFutures.add(client.discoverData(rule.getKey())
                                .thenAccept(list -> {
                                    try {
                                        processEntries(client, rule, list, collectionSetBuilder, nodeResource, resourceTypeGenerator);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }));
                    } catch (ZabbixNotSupportedException | IOException e) {
                        // pass
                    }
                }
                CompletableFuture.allOf(templateFutures.toArray(new CompletableFuture[0])).get();
            }
            collectionSetBuilder.addCollectionSetResource(nodeResourceBuilder.build());
            future.complete(collectionSetBuilder.build());
        } catch (IOException | NumberFormatException e) {
            future.completeExceptionally(e);
        } catch (Exception e) {
            LOG.error("Error during collection for request: {}", collectionRequest, e);
            future.completeExceptionally(e);
        }
        return future;
    }


    private CompletableFuture<Void> processItems(ZabbixAgentClient client, DiscoveryRule rule, List<Item> items, ImmutableCollectionSetResource.Builder<?> resourceBuilder) throws IOException {
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (Item item : items) {
            futureList.add(client.retrieveData(item.getKey()).thenAcceptAsync(value -> addValueToMapper(rule, item, value, resourceBuilder)));
        }
        return CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> processEntries(ZabbixAgentClient client, DiscoveryRule rule, List<Map<String, Object>> entries,
                                                   ImmutableCollectionSet.Builder collectionSetBuilder, NodeResource nodeResource, ZabbixResourceTypeGenerator resourceTypeGenerator) throws IOException {
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
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
            futureList.add(processItems(client, rule, rule.getItemPrototypes(), resourceBuilder).thenRunAsync(() -> {
                if (rule.getItemPrototypes().size() > 0) {
                    collectionSetBuilder.addCollectionSetResource(resourceBuilder.build());
                }
            }));
        }
        return CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
    }

    private void addValueToMapper(DiscoveryRule rule, Item item, String value, ImmutableCollectionSetResource.Builder<?> resourceBuilder) {
        if (value.startsWith(ZabbixAgentClient.UNSUPPORTED_HEADER)) {
            LOG.error("{} <> {}", item.getKey(), value);
        } else {
            metricMapper.addValueToResource(rule, item, value, resourceBuilder);
        }
    }

    @Override
    public void initialize() {
        // pass
    }
}
