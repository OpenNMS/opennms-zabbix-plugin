package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
import org.opennms.plugins.zabbix.mock.MockZabbixAgent;

import com.google.common.collect.ImmutableMap;

public class ZabbixAgentCollectorTest {

    @Rule
    public MockZabbixAgent zabbixAgent = new MockZabbixAgent();

    @Test
    public void canCollectCpuDetails() throws ExecutionException, InterruptedException, TimeoutException {
        CollectionRequest request = mock(CollectionRequest.class);
        when(request.getAddress()).thenReturn(zabbixAgent.getAddress());
        ZabbixAgentCollector collector = new ZabbixAgentCollector();

        Map<String, Object> collectorOptions = ImmutableMap.of(ZabbixAgentCollector.PORT_KEY, zabbixAgent.getPort());
        CompletableFuture<CollectionSet> future = collector.collect(request, collectorOptions);

        // Verify
        CollectionSet collectionSet = future.get(5, TimeUnit.SECONDS);
        // Expect many resources
        assertThat(collectionSet.getCollectionSetResources(), hasSize(15));
    }
}
