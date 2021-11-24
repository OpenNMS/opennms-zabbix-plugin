package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.core.util.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.collectors.resource.GenericTypeResource;
import org.opennms.integration.api.v1.collectors.resource.NodeResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSet;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSetResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableNodeResource;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Template;

import com.google.common.collect.ImmutableMap;

public class WinZabbixAgentCollectorTest {

    private static List<Template> allTemplates;
    @Rule
    public WinZabbixAgentResource zabbixAgent = new WinZabbixAgentResource();
    private InetAddress address;
    private int port;
    private String windowsServiceEntries;

    @BeforeClass
    public static void setup() {
        allTemplates = new ZabbixTemplateHandler().getTemplates();
    }

    @Before
    public void init() throws Exception {
        address = zabbixAgent.getAddress();
        port = zabbixAgent.getPort();
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("win_service.txt");
        if(in != null) {
            windowsServiceEntries = IOUtils.toString(new InputStreamReader(in));
        }
    }

    @Test
    public void canCollectCpuDetailsWin() throws ExecutionException, InterruptedException, TimeoutException {
        CollectionRequest request = mock(CollectionRequest.class);
        when(request.getAddress()).thenReturn(address);

        NodeDao nodeDao = mock(NodeDao.class);
        //ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = mock(TemplateResolver.class);
        List<Template> windowsTemplates = getTemplates();
        when(templateResolver.getTemplatesForNode(null)).thenReturn(windowsTemplates);
        ZabbixAgentClientFactory clientFactory = new ZabbixAgentClientFactory();
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
        CompletableFuture<CollectionSet> future = collector.collect(request, collectorOptions);

        // Verify
        CollectionSet collectionSet = future.get(5, TimeUnit.SECONDS);
        // Expect many resources
        assertThat(collectionSet.getCollectionSetResources().size(), is(66));
    }

    @Test
    public void testWithMockClient() throws ExecutionException, InterruptedException, TimeoutException {
        CollectionRequest request = mock(CollectionRequest.class);
        when(request.getAddress()).thenReturn(address);

        NodeDao nodeDao = mock(NodeDao.class);
        //ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = mock(TemplateResolver.class);
        List<Template> windowsTemplates = getTemplates();
        when(templateResolver.getTemplatesForNode(null)).thenReturn(windowsTemplates);
        ZabbixAgentClient client = new ZabbixAgentClientFactory().createClient(address, port);
        ZabbixAgentClientFactory mockClientFactory = mock(ZabbixAgentClientFactory.class);
        ZabbixAgentClient mockClient = createMockClient();
        when(mockClientFactory.createClient(any(InetAddress.class), anyInt())).thenReturn(mockClient);

        ZabbixAgentCollectorFactory zabbixAgentCollectorFactory = new ZabbixAgentCollectorFactory(nodeDao, templateResolver);
        zabbixAgentCollectorFactory.setClientFactory(mockClientFactory);
        Map<String, Object> runtimeAttributes = zabbixAgentCollectorFactory.getRuntimeAttributes(request);
        ZabbixAgentCollector collector = new ZabbixAgentCollector(mockClientFactory);
        Map<String, Object> collectorOptions = ImmutableMap.<String, Object>builder()
                .put(ZabbixAgentCollector.PORT_KEY, port)
                .putAll(runtimeAttributes)
                .build();

        // marshal/unmarshal for test coverage
        collectorOptions = zabbixAgentCollectorFactory.unmarshalParameters(zabbixAgentCollectorFactory.marshalParameters(collectorOptions));
        CompletableFuture<CollectionSet> future = collector.collect(request, collectorOptions);

        // Verify
        CollectionSet collectionSet = future.get(15, TimeUnit.SECONDS);
        // Expect many resources
        assertThat(collectionSet.getCollectionSetResources().size(), is(373));

    }

    @Test
    public void testProcessEntries() {
        String netDiscovery = "service.discovery";
        DiscoveryRule rule = getTemplates().stream().flatMap(t -> t.getDiscoveryRules().stream().filter(r -> r.getKey().equals(netDiscovery))).findFirst().orElse(null);
        ZabbixAgentClient mockClient = createMockClient();
        ZabbixAgentClientFactory mockClientFactory = mock(ZabbixAgentClientFactory.class);
        when(mockClientFactory.createClient(address, port)).thenReturn(mockClient);
        ZabbixAgentCollector collector = new ZabbixAgentCollector(mockClientFactory);
        ZabbixResourceTypeGenerator resourceTypeGenerator = new ZabbixResourceTypeGenerator();

        List<Map<String, Object>> entries = mockClient.discoverData(netDiscovery).join();


        ImmutableCollectionSet.Builder collectionSetBuilder = ImmutableCollectionSet.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setStatus(CollectionSet.Status.SUCCEEDED);
        NodeResource nodeResource = ImmutableNodeResource.newBuilder().setNodeId(0).build();

        List<ImmutableCollectionSetResource.Builder<GenericTypeResource>> resourceBuilders = new ArrayList<>();

        collector.processEntries(mockClient, rule, entries, nodeResource, resourceBuilders).join();
        resourceBuilders.forEach(res->collectionSetBuilder.addCollectionSetResource(res.build()));
        System.out.println(collectionSetBuilder.build().getCollectionSetResources().size());
        assertThat(collectionSetBuilder.build().getCollectionSetResources().size(), is(307));
    }

    private List<Template> getTemplates() {
        return allTemplates.stream().filter(t -> t.getName().startsWith("Windows")).collect(Collectors.toList());
    }

    private ZabbixAgentClient createMockClient() {
        ZabbixAgentClient client = new ZabbixAgentClientFactory().createClient(address, port);
        ZabbixAgentClient mockClient = spy(client);
        when(mockClient.retrieveData(anyString()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    switch (key) {
                        case "agent.version":
                            return CompletableFuture.completedFuture("5.4.7");
                        case "vfs.fs.size[/,pfree]":
                            return CompletableFuture.completedFuture("90");
                        case "vfs.fs.discovery":
                            return CompletableFuture.completedFuture("[{\"{#FSNAME}\":\"C:\",\"{#FSTYPE}\":\"NTFS\",\"{#FSDRIVETYPE}\":\"fixed\"}]");
                        case "net.if.discovery":
                            return CompletableFuture.completedFuture("[{\"{#IFNAME}\":\"WAN Miniport (IP)-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C35C-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Switch Extension Adapter-Hyper-V Virtual Switch Extension Filter-0000\",\"{#IFGUID}\":\"{AC28C3DE-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Switch Extension Adapter #3-Hyper-V Virtual Switch Extension Filter-0000\",\"{#IFGUID}\":\"{C580658E-F0A9-11EB-A268-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Switch Extension Adapter #2-Hyper-V Virtual Switch Extension Filter-0000\",\"{#IFGUID}\":\"{AC28C7BA-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Intel(R) Ethernet Connection (10) I219-LM-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C1CA-32B8-11EC-A277-806E6F6E6963}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{F3E266F2-3D78-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{F3E266F3-3D78-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter-WFP 802.3 MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{F3E266F4-3D78-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Switch Extension Adapter #4-Hyper-V Virtual Switch Extension Filter-0000\",\"{#IFGUID}\":\"{A657F9FC-283F-11EC-A272-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #2-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{447567A8-3774-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #3-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C1D0-32B8-11EC-A277-806E6F6E6963}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #2-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{447567A9-3774-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #3-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{AC28C1D2-32B8-11EC-A277-806E6F6E6963}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #4-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C1D3-32B8-11EC-A277-806E6F6E6963}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #4-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{AC28C1D4-32B8-11EC-A277-806E6F6E6963}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #3-WFP 802.3 MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C1D5-32B8-11EC-A277-806E6F6E6963}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #4-WFP 802.3 MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C1D6-32B8-11EC-A277-806E6F6E6963}\"},{\"{#IFNAME}\":\"WAN Miniport (IP)-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{AC28C35D-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"WAN Miniport (IPv6)-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C363-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"WAN Miniport (IPv6)-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{AC28C364-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"WAN Miniport (Network Monitor)-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C369-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"WAN Miniport (Network Monitor)-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{AC28C36A-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #2-WFP 802.3 MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{447567AA-3774-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Microsoft Kernel Debug Network Adapter\",\"{#IFGUID}\":\"{522EC1CD-0F35-453D-AE55-B8AE6E6EB2F5}\"},{\"{#IFNAME}\":\"WAN Miniport (IP)\",\"{#IFGUID}\":\"{0C5A557F-2A1D-4A73-85EE-9DAF007D7E77}\"},{\"{#IFNAME}\":\"WAN Miniport (IPv6)\",\"{#IFGUID}\":\"{EA1EF15C-BA3C-4691-8468-D2A620D27217}\"},{\"{#IFNAME}\":\"WAN Miniport (Network Monitor)\",\"{#IFGUID}\":\"{E05049C3-5E4B-493D-984F-E0C5DD4D11A7}\"},{\"{#IFNAME}\":\"Intel(R) Ethernet Connection (10) I219-LM\",\"{#IFGUID}\":\"{2A64D373-AAA8-4256-B239-9B6D3ADE8DDA}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Switch Extension Adapter\",\"{#IFGUID}\":\"{E0D74FDC-8E50-42C6-BB13-DA6DB2819590}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter\",\"{#IFGUID}\":\"{4346262F-50B4-43F5-A410-C0CDD279C9CE}\"},{\"{#IFNAME}\":\"Bluetooth Device (Personal Area Network)\",\"{#IFGUID}\":\"{F086129F-8D3A-4B79-9487-25C1C050F3E4}\"},{\"{#IFNAME}\":\"Cisco AnyConnect Secure Mobility Client Virtual Miniport Adapter for Windows x64\",\"{#IFGUID}\":\"{E9685CEE-C5B3-485D-86B3-D15E3240B677}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Switch Extension Adapter #2\",\"{#IFGUID}\":\"{4703D4F2-DCF7-4899-B951-C76C66053E04}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #2\",\"{#IFGUID}\":\"{E241E526-50A3-4CC5-B829-42765A9045F7}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Switch Extension Adapter #3\",\"{#IFGUID}\":\"{EB543C7D-380D-4AC3-9920-11AED97C410B}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #3\",\"{#IFGUID}\":\"{489B7990-1983-4B06-81AA-8BD0304EA629}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Switch Extension Adapter #4\",\"{#IFGUID}\":\"{588A69EF-0F92-4A63-B12B-9816B596008D}\"},{\"{#IFNAME}\":\"Hyper-V Virtual Ethernet Adapter #4\",\"{#IFGUID}\":\"{0CE4653C-6EBA-47BD-BE21-3900009727AC}\"},{\"{#IFNAME}\":\"WAN Miniport (PPPOE)\",\"{#IFGUID}\":\"{A177DC98-D8B7-43A4-BC0F-F8CF913F831B}\"},{\"{#IFNAME}\":\"Software Loopback Interface 1\",\"{#IFGUID}\":\"{1C04FBD8-203E-11E8-9897-806E6F6E6963}\"},{\"{#IFNAME}\":\"Intel(R) Wi-Fi 6 AX201 160MHz-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C304-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Intel(R) Wi-Fi 6 AX201 160MHz-Virtual WiFi Filter Driver-0000\",\"{#IFGUID}\":\"{4C88D708-BEED-11EB-A258-00059A3C7A00}\"},{\"{#IFNAME}\":\"Intel(R) Wi-Fi 6 AX201 160MHz-Native WiFi Filter Driver-0000\",\"{#IFGUID}\":\"{4C88D709-BEED-11EB-A258-00059A3C7A00}\"},{\"{#IFNAME}\":\"Intel(R) Wi-Fi 6 AX201 160MHz-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{AC28C306-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Intel(R) Wi-Fi 6 AX201 160MHz-WFP 802.3 MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AC28C307-32B8-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #3-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AEABD36E-3C0B-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #2-WFP Native MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AEABD2E3-3C0B-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #2-Native WiFi Filter Driver-0000\",\"{#IFGUID}\":\"{4C88D71D-BEED-11EB-A258-00059A3C7A00}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #2-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{AEABD2E4-3C0B-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #2-WFP 802.3 MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AEABD2E5-3C0B-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #3-Native WiFi Filter Driver-0000\",\"{#IFGUID}\":\"{57F61BEC-BEE5-11EB-A25A-3C58C27671B2}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #3-QoS Packet Scheduler-0000\",\"{#IFGUID}\":\"{AEABD36F-3C0B-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #3-WFP 802.3 MAC Layer LightWeight Filter-0000\",\"{#IFGUID}\":\"{AEABD370-3C0B-11EC-A277-3C58C27671B2}\"},{\"{#IFNAME}\":\"Intel(R) Wi-Fi 6 AX201 160MHz\",\"{#IFGUID}\":\"{9E055665-8837-459C-9965-2EAB68D5F3E6}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter\",\"{#IFGUID}\":\"{295DB050-7378-4B4D-8723-63B38A5F59FA}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #2\",\"{#IFGUID}\":\"{03926915-C5BC-4B43-9BC1-9454AB07DB2A}\"},{\"{#IFNAME}\":\"Microsoft Wi-Fi Direct Virtual Adapter #3\",\"{#IFGUID}\":\"{BF93F715-8ED1-4ADA-8FD6-8741240380B0}\"},{\"{#IFNAME}\":\"Microsoft Teredo Tunneling Adapter\",\"{#IFGUID}\":\"{93123211-9629-4E04-82F0-EA2E4F221468}\"},{\"{#IFNAME}\":\"Microsoft IP-HTTPS Platform Adapter\",\"{#IFGUID}\":\"{2EE2C70C-A092-4D88-A654-98C8D7645CD5}\"},{\"{#IFNAME}\":\"Microsoft 6to4 Adapter\",\"{#IFGUID}\":\"{07374750-E68B-490E-9330-9FD785CD71B6}\"},{\"{#IFNAME}\":\"WAN Miniport (SSTP)\",\"{#IFGUID}\":\"{F4153B0E-BB8B-4024-9D6A-AE5BC91577F3}\"},{\"{#IFNAME}\":\"WAN Miniport (IKEv2)\",\"{#IFGUID}\":\"{97709629-045D-4A4C-9BEA-A3BCC44F7CB9}\"},{\"{#IFNAME}\":\"WAN Miniport (L2TP)\",\"{#IFGUID}\":\"{F9F20FEA-2E69-4EEE-AD4D-11038DF15C88}\"},{\"{#IFNAME}\":\"WAN Miniport (PPTP)\",\"{#IFGUID}\":\"{4C9A8026-28CE-4B39-8B74-0345E1963686}\"}]\n");
                        case "service.discovery":
                            return CompletableFuture.completedFuture(windowsServiceEntries);
                        default:
                            return CompletableFuture.completedFuture("ZBX_NOTSUPPORTED oops");
                    }
                });
        return mockClient;
    }
}
