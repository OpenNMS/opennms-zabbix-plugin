package org.opennms.plugins.zabbix;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.collectors.ServiceCollector;
import org.opennms.integration.api.v1.collectors.immutables.ImmutableNumericAttribute;
import org.opennms.integration.api.v1.collectors.immutables.ImmutableStringAttribute;
import org.opennms.integration.api.v1.collectors.resource.GenericTypeResource;
import org.opennms.integration.api.v1.collectors.resource.NodeResource;
import org.opennms.integration.api.v1.collectors.resource.NumericAttribute;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSet;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSetResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableGenericTypeResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableNodeResource;
import org.opennms.integration.api.v1.config.datacollection.ResourceType;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.ItemPrototype;
import org.opennms.plugins.zabbix.model.Template;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Make collector async (required async client)
 * TODO: Collect multiple keys in parallel, leveraging async behavior
 * TODO: Determine which templates are applicable to the agent - don't just process all of them
 */
public class ZabbixAgentCollector implements ServiceCollector {

    public static final String PORT_KEY = "port";

    private static final Logger LOG = LoggerFactory.getLogger(ZabbixAgentCollector.class);

    private final BundleContext bundleContext;

    public ZabbixAgentCollector() {
        this(null);
    }

    public ZabbixAgentCollector(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public CompletableFuture<CollectionSet> collect(CollectionRequest collectionRequest, Map<String, Object> map) {
        final CompletableFuture<CollectionSet> future = new CompletableFuture<>();

        int port = ZabbixAgentClient.DEFAULT_PORT;
        if (map.containsKey(PORT_KEY)) {
            // FIXME: Handle string values too -_-
            port = (int)map.get(PORT_KEY);
        }

        try (ZabbixAgentClient client = new ZabbixAgentClient(collectionRequest.getAddress(), port)) {
            String version = "<unknown>";
            try {
                version = client.retrieveData("agent.version");
            } catch (ZabbixNotSupportedException e) {
                // pass
            }
            double vmMemoryBytes = Double.NaN;
            try {
                vmMemoryBytes = Double.parseDouble(client.retrieveData("vm.memory.size"));
            } catch (ZabbixNotSupportedException e) {
                // pass
            }
            double vmMemoryFreeBytes = Double.NaN;
            try {
                vmMemoryFreeBytes = Double.parseDouble(client.retrieveData("vm.memory.size[free]"));
            } catch (ZabbixNotSupportedException e) {
                // pass
            }
            double vmMemoryPercentageAvailable = Double.NaN;
            try {
                vmMemoryPercentageAvailable = Double.parseDouble(client.retrieveData("vm.memory.size[pavailable]"));
            } catch (ZabbixNotSupportedException e) {
                // pass
            }

            NodeResource nodeResource = ImmutableNodeResource.newBuilder().setNodeId(1).build();
            ImmutableCollectionSetResource.Builder<NodeResource> builder = ImmutableCollectionSetResource.newBuilder(NodeResource.class)
                    .setResource(nodeResource)
                    .addStringAttribute(ImmutableStringAttribute.newBuilder()
                            .setGroup("zabbix").setName("agent-version").setValue(version).build())
                    .addNumericAttribute(ImmutableNumericAttribute.newBuilder()
                            .setGroup("zabbix").setName("mem-total-bytes").setValue(vmMemoryBytes).setType(NumericAttribute.Type.GAUGE).build())
                    .addNumericAttribute(ImmutableNumericAttribute.newBuilder()
                            .setGroup("zabbix").setName("mem-free-bytes").setValue(vmMemoryFreeBytes).setType(NumericAttribute.Type.GAUGE).build())
                    .addNumericAttribute(ImmutableNumericAttribute.newBuilder()
                            .setGroup("zabbix").setName("mem-perc-avail").setValue(vmMemoryPercentageAvailable).setType(NumericAttribute.Type.GAUGE).build());

            ImmutableCollectionSet.Builder collectionSetBuilder = ImmutableCollectionSet.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .setStatus(CollectionSet.Status.SUCCEEDED);

            // Process all the templates
            final ZabbixTemplateHandler templateHandler = new ZabbixTemplateHandler(bundleContext);
            final List<Template> templates = templateHandler.getTemplates();
            for (Template template : templates) {
                System.out.printf("Template[%s]: %s\n", template.getName(), template.getDescription());
                for (String key : templateHandler.getKeys(template)) {
                    try {
                        String value = client.retrieveData(key);
                        builder.addStringAttribute(ImmutableStringAttribute.newBuilder()
                                .setGroup("zabbix").setName(key).setValue(value).build());
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

                        System.out.println("DATA for key " + rule.getKey() + ": " + entries);
                        for (Map<String, Object> entry : entries) {
                            // Resource entry
                            ImmutableCollectionSetResource.Builder<GenericTypeResource> resourceBuilder = ImmutableCollectionSetResource.newBuilder(GenericTypeResource.class)
                                    .setResource(ImmutableGenericTypeResource.newBuilder()
                                            .setNodeResource(nodeResource)
                                            .setType(resourceType.getName())
                                            .setInstance(resourceTypeGenerator.getIndex(rule, entry))
                                            .build());

                            // an entry looks something like: {"{#CPU.NUMBER}":0,"{#CPU.STATUS}":"online"}
                            for (ItemPrototype item : rule.getItemPrototypes()) {
                                final String effectiveKey = ZabbixMacroSupport.evaluateMacro(item.getKey(), entry);
                                try {
                                    System.out.println("Discovering KEY " + effectiveKey);
                                    String value = client.retrieveData(effectiveKey);
                                    resourceBuilder.addStringAttribute(ImmutableStringAttribute.newBuilder()
                                            .setGroup("zabbix").setName(effectiveKey).setValue(value).build());
                                } catch (ZabbixNotSupportedException e) {
                                    // pass
                                }
                            }

                            collectionSetBuilder.addCollectionSetResource(resourceBuilder.build());
                        }
                    } catch (ZabbixNotSupportedException e) {
                        // pass
                    }
                }
            }
            collectionSetBuilder.addCollectionSetResource(builder.build());
            future.complete(collectionSetBuilder.build());
        } catch (IOException|NumberFormatException e) {
            future.completeExceptionally(e);
        } catch (Exception e) {
            LOG.error("oops", e);
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void initialize() {
        // pass
    }
}
