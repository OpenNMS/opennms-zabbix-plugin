package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.collectors.resource.CollectionSetResource;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.plugins.zabbix.model.Template;

import com.google.common.collect.ImmutableMap;

public class ZabbixAgentCollectorTest {

    @Rule
    public MockZabbixAgent zabbixAgent = new MockZabbixAgent();

    private static List<Template> allTemplates;
    @BeforeClass
    public static void setup() {
        allTemplates = new ZabbixTemplateHandler().getTemplates();
    }

    @Ignore
    @Test
    public void canCollectCpuDetails() throws ExecutionException, InterruptedException, TimeoutException {
        CollectionRequest request = mock(CollectionRequest.class);
        when(request.getAddress()).thenReturn(zabbixAgent.getAddress());

        NodeDao nodeDao = mock(NodeDao.class);
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = mock(TemplateResolver.class);
        when(templateResolver.getTemplatesForNode(null)).thenReturn(zabbixTemplateHandler.getTemplates());
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
        CollectionSet collectionSet = future.get(15, TimeUnit.SECONDS);
        // Expect many resources
        assertThat(collectionSet.getCollectionSetResources().size(), is(15));
    }

    @Ignore
    @Test
    public void canCollectCpuDetailsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        CollectionRequest request = mock(CollectionRequest.class);
        when(request.getAddress()).thenReturn(zabbixAgent.getAddress());

        NodeDao nodeDao = mock(NodeDao.class);
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = mock(TemplateResolver.class);
        when(templateResolver.getTemplatesForNode(null)).thenReturn(zabbixTemplateHandler.getTemplates());
        ZabbixAgentClientFactory clientFactory =new ZabbixAgentClientFactory();
        ZabbixAgentCollectorFactory zabbixAgentCollectorFactory = new ZabbixAgentCollectorFactory(nodeDao, templateResolver);
        zabbixAgentCollectorFactory.setClientFactory(clientFactory);
        Map<String, Object> runtimeAttributes = zabbixAgentCollectorFactory.getRuntimeAttributes(request);
        ZabbixAgentCollectorAsync collector = new ZabbixAgentCollectorAsync(clientFactory);
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


    @Test
    public void canCollectCpuDetailsWin() throws ExecutionException, InterruptedException, TimeoutException, UnknownHostException {
        InetAddress address = InetAddress.getByName("172.20.80.1");
        int port = 10050;
        CollectionRequest request = mock(CollectionRequest.class);
        when(request.getAddress()).thenReturn(address);

        NodeDao nodeDao = mock(NodeDao.class);
        //ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = mock(TemplateResolver.class);
        List<Template> windowsTemplates = getTemplates();
        when(templateResolver.getTemplatesForNode(null)).thenReturn(windowsTemplates);
        ZabbixAgentClientFactory clientFactory =new ZabbixAgentClientFactory();
        ZabbixAgentCollectorFactory zabbixAgentCollectorFactory = new ZabbixAgentCollectorFactory(nodeDao, templateResolver);
        zabbixAgentCollectorFactory.setClientFactory(clientFactory);
        Map<String, Object> runtimeAttributes = zabbixAgentCollectorFactory.getRuntimeAttributes(request);
        ZabbixAgentCollector collector = zabbixAgentCollectorFactory.createCollector();
        Map<String, Object> collectorOptions = ImmutableMap.<String, Object>builder()
                .put(ZabbixAgentCollector.PORT_KEY, port)
                .putAll(runtimeAttributes)
                .build();

        // marshal/unmarshal for test coverage
        collectorOptions = zabbixAgentCollectorFactory.unmarshalParameters(zabbixAgentCollectorFactory.marshalParameters(collectorOptions));
        long start = System.currentTimeMillis();
        CompletableFuture<CollectionSet> future = collector.collect(request, collectorOptions);

        // Verify
        CollectionSet collectionSet = future.get(15, TimeUnit.SECONDS);
        // Expect many resources
        List<CollectionSetResource> list = collectionSet.getCollectionSetResources();
        long end = System.currentTimeMillis();
        System.out.println(String.format("Sync version takes %d seconds to collect %d items", end-start, list.size()));
        //assertThat(collectionSet.getCollectionSetResources().size(), is(15));
    }

    @Test
    public void canCollectCpuDetailsAsyncWin() throws ExecutionException, InterruptedException, TimeoutException, UnknownHostException {
        InetAddress address = InetAddress.getByName("172.20.80.1");
        int port = 10050;
        CollectionRequest request = mock(CollectionRequest.class);
        when(request.getAddress()).thenReturn(address);

        NodeDao nodeDao = mock(NodeDao.class);
        //ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = mock(TemplateResolver.class);
        List<Template> windowsTemplates = getTemplates();
        when(templateResolver.getTemplatesForNode(null)).thenReturn(windowsTemplates);
        ZabbixAgentClientFactory clientFactory =new ZabbixAgentClientFactory();
        ZabbixAgentCollectorFactory zabbixAgentCollectorFactory = new ZabbixAgentCollectorFactory(nodeDao, templateResolver);
        zabbixAgentCollectorFactory.setClientFactory(clientFactory);
        Map<String, Object> runtimeAttributes = zabbixAgentCollectorFactory.getRuntimeAttributes(request);
        ZabbixAgentCollectorAsync collector = new ZabbixAgentCollectorAsync(clientFactory);
        Map<String, Object> collectorOptions = ImmutableMap.<String, Object>builder()
                .put(ZabbixAgentCollector.PORT_KEY, port)
                .putAll(runtimeAttributes)
                .build();

        // marshal/unmarshal for test coverage
        collectorOptions = zabbixAgentCollectorFactory.unmarshalParameters(zabbixAgentCollectorFactory.marshalParameters(collectorOptions));
        long start = System.currentTimeMillis();
        CompletableFuture<CollectionSet> future = collector.collect(request, collectorOptions);

        // Verify
        CollectionSet collectionSet = future.get(15, TimeUnit.SECONDS);
        // Expect many resources
        //assertThat(collectionSet.getCollectionSetResources().size(), is(15));
        List<CollectionSetResource> list = collectionSet.getCollectionSetResources();
        long end = System.currentTimeMillis();
        System.out.println(String.format("Async takes %d seconds to collect %d items", end-start, list.size()));
    }

    private List<Template> getTemplates() {
        List<String> names = Arrays.asList(/*"Windows by Zabbix agent", "Windows CPU by Zabbix agent", "Windows filesystems by Zabbix agent", "Windows generic by Zabbix agent",
                "Windows memory by Zabbix agent",*/ "Windows network by Zabbix agent");
        return allTemplates.stream().filter(t-> names.contains(t.getName())).collect(Collectors.toList());
    }
}
