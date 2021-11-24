package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.plugins.zabbix.model.Template;

import com.google.common.collect.ImmutableMap;

public class ZabbixAgentCollectorTest {

    @Rule
    public LinuxZabbixAgentResource zabbixAgent = new LinuxZabbixAgentResource();

    private static List<Template> allTemplates;
    @BeforeClass
    public static void setup() {
        allTemplates = new ZabbixTemplateHandler().getTemplates();
    }

    @Test
    public void canCollectCpuDetails() throws ExecutionException, InterruptedException, TimeoutException {
        CollectionRequest request = mock(CollectionRequest.class);
        when(request.getAddress()).thenReturn(zabbixAgent.getAddress());

        NodeDao nodeDao = mock(NodeDao.class);
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = mock(TemplateResolver.class);
        when(templateResolver.getTemplatesForNode(null)).thenReturn(getTemplates());
        ZabbixAgentClientFactory clientFactory =new ZabbixAgentClientFactory();
        ZabbixAgentCollectorFactory zabbixAgentCollectorFactory = new ZabbixAgentCollectorFactory(nodeDao, templateResolver);
        zabbixAgentCollectorFactory.setClientFactory(clientFactory);
        Map<String, Object> runtimeAttributes = zabbixAgentCollectorFactory.getRuntimeAttributes(request);
        ZabbixAgentCollector collector = zabbixAgentCollectorFactory.createCollector();
        Map<String, Object> collectorOptions = ImmutableMap.<String, Object>builder()
                .put(ZabbixAgentCollector.PORT_KEY, zabbixAgent.getPort())
                .putAll(runtimeAttributes)
                .build();

        // marshal/unmarshal for test coverage
        collectorOptions = zabbixAgentCollectorFactory.unmarshalParameters(zabbixAgentCollectorFactory.marshalParameters(collectorOptions));

        CompletableFuture<CollectionSet> future = collector.collect(request, collectorOptions);

        // Verify
        CollectionSet collectionSet = future.get(5, TimeUnit.SECONDS);
        // Expect many resources
        assertThat(collectionSet.getCollectionSetResources().size(), is(8));
    }

    private List<Template> getTemplates() {
        return allTemplates.stream().filter(t-> !t.getName().startsWith("Windows")).collect(Collectors.toList());
    }
}
