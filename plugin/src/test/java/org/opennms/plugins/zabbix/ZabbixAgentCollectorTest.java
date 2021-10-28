package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.dao.NodeDao;

import com.google.common.collect.ImmutableMap;

public class ZabbixAgentCollectorTest {

    @Rule
    public MockZabbixAgent zabbixAgent = new MockZabbixAgent();
    private int threadSize = 10;
    private int poolSize = 50;

    @Test
    public void canCollectCpuDetails() throws ExecutionException, InterruptedException, TimeoutException {
        CollectionRequest request = mock(CollectionRequest.class);
        when(request.getAddress()).thenReturn(zabbixAgent.getAddress());

        NodeDao nodeDao = mock(NodeDao.class);
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = mock(TemplateResolver.class);
        when(templateResolver.getTemplatesForNode(null)).thenReturn(zabbixTemplateHandler.getTemplates());
        ZabbixAgentClientFactory clientFactory =new ZabbixAgentClientFactory(threadSize, poolSize);
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
        CollectionSet collectionSet = future.get(15, TimeUnit.SECONDS);
        // Expect many resources
        assertThat(collectionSet.getCollectionSetResources().size(), is(15));
    }
}
