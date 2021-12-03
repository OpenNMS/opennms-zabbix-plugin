package org.opennms.plugins.zabbix.lab;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.collectors.CollectionSetPersistenceService;
import org.opennms.integration.api.v1.collectors.ServiceCollectorFactory;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.events.EventForwarder;
import org.opennms.integration.api.v1.events.EventListener;
import org.opennms.integration.api.v1.events.EventSubscriptionService;
import org.opennms.integration.api.v1.model.InMemoryEvent;

import org.opennms.integration.api.v1.model.Node;
import org.opennms.integration.api.v1.model.immutables.ImmutableEventParameter;
import org.opennms.integration.api.v1.model.immutables.ImmutableInMemoryEvent;
import org.opennms.plugins.zabbix.ZabbixAgentClient;
import org.opennms.plugins.zabbix.ZabbixAgentCollector;
import org.opennms.plugins.zabbix.ZabbixAgentCollectorFactory;
import org.opennms.plugins.zabbix.agent.ZabbixAgent;

import com.google.common.collect.ImmutableMap;

@Command(scope = "opennms-zabbix", name = "start-lab", description="Start a new lab")
@Service
public class StartLab implements Action {

    public static final String IMPORT_STARTED_UEI = "uei.opennms.org/internal/importer/importStarted";
    public static final String PARM_IMPORT_RESOURCE = "importResource";

    @Reference
    private LabContextManager labContextManager;

    @Reference
    private NodeDao nodeDao;

    @Reference
    private EventForwarder eventForwarder;

    @Reference
    private EventSubscriptionService eventSubscriptionService;

    @Reference(filter = "(type=zabbix)")
    private ServiceCollectorFactory<ZabbixAgentCollector> zabbixServiceCollectorFactory;

    @Reference
    private CollectionSetPersistenceService collectionSetPersistenceService;

    @Override
    public Object execute() throws UnknownHostException, InterruptedException, ExecutionException, TimeoutException {
        try (LabContextManager.LabSession labSession = labContextManager.newSession()) {
            final String sessionName = "zabbix-lab-" + labSession.getSessionId();
            System.out.printf("Created session %s\n", sessionName);

            final ZabbixAgent agent = new ZabbixAgent();
            System.out.println("Starting agent...");
            agent.start();
            System.out.printf("Agent started on %s:%d\n", agent.getAddress().getHostAddress(), agent.getPort());

            System.out.printf("Creating requisition %s\n", sessionName);
            // Verify that no nodes are currently present for the foreign source
            List<Node> nodes = nodeDao.getNodesInForeignSource(sessionName);
            if (!nodes.isEmpty()) {
                throw new RuntimeException(String.format("Expected to find 0 nodes in foreign-source %s, but found %d.\n",
                        sessionName, nodes.size()));
            }

            final String url = String.format("requisition://%s?foreignSource=%s&sessionId=%s&agentIp=%s", LabRequisitionProvider.TYPE,
                    sessionName, labSession.getSessionId(), agent.getAddress().getHostAddress());
            try (ImportStartedEventHandler eventHandler = new ImportStartedEventHandler(eventSubscriptionService, labSession.getSessionId())) {
                // Import the requisition
                final InMemoryEvent reloadImport = ImmutableInMemoryEvent.newBuilder()
                        .setUei("uei.opennms.org/internal/importer/reloadImport")
                        .setSource(StartLab.class.getCanonicalName())
                        .addParameter(ImmutableEventParameter.newInstance("url", url))
                        .build();
                eventForwarder.sendSync(reloadImport);
                // Wait until we get a import start event
                eventHandler.waitForImportStarted();
            }

            // Wait until our provisioning extension was triggered
            System.out.println("Waiting for requisition to begin importing...");
            labSession.waitForGet();

            // Wait for the node to appear
            System.out.println("Waiting for node to be imported...");
            while (true) {
                nodes = nodeDao.getNodesInForeignSource(sessionName);
                if (nodes.isEmpty()) {
                    Thread.sleep(500);
                }
                break;
            }
            Node node = nodes.get(0);
            System.out.printf("Node with id=%s and label=%s is now present.\n", node.getId(), node.getLabel());

            // Trigger the collection
            ZabbixAgentCollector collector = zabbixServiceCollectorFactory.createCollector();
            CollectionRequest collectionRequest = new CollectionRequest() {
                @Override
                public InetAddress getAddress() {
                    return agent.getAddress();
                }

                @Override
                public int getNodeId() {
                    return node.getId();
                }
            };
            final Map<String,Object> collectionParams = ImmutableMap.<String,Object>builder()
                            .put(ZabbixAgentCollector.PORT_KEY, agent.getPort())
                            .putAll(zabbixServiceCollectorFactory.getRuntimeAttributes(collectionRequest, null))
                            .build();

            for (int i = 0; i < 2; i++) {
                if (i != 0) {
                    System.out.println("Sleeping 5 seconds before next collection attempt.");
                    Thread.sleep(5000);
                }
                System.out.println("Triggering collection...");
                CompletableFuture<CollectionSet> future = collector.collect(collectionRequest, collectionParams);
                System.out.println("Waiting for collection to complete...");
                CollectionSet collectionSet = future.get(2, TimeUnit.MINUTES);
                System.out.println("Collection complete. Persisting...");
                collectionSetPersistenceService.persist(node.getId(), agent.getAddress(), collectionSet);
                System.out.println("Persistence complete.");
            }

            System.out.println("Waiting for threshold alarm...");
            Thread.sleep(1000);
            throw new RuntimeException("No threshold alarm found!");
        }
    }

    private static class ImportStartedEventHandler implements EventListener, AutoCloseable {
        private final EventSubscriptionService eventSubscriptionService;
        private final String sessionId;
        private CountDownLatch startedLatch = new CountDownLatch(1);

        public ImportStartedEventHandler(EventSubscriptionService eventSubscriptionService, String sessionId) {
            this.eventSubscriptionService = Objects.requireNonNull(eventSubscriptionService);
            this.sessionId = Objects.requireNonNull(sessionId);
            eventSubscriptionService.addEventListener(this, IMPORT_STARTED_UEI);
        }

        @Override
        public String getName() {
            return ImportStartedEventHandler.class.getCanonicalName() + "-" + sessionId;
        }

        @Override
        public int getNumThreads() {
            return 1;
        }

        @Override
        public void onEvent(InMemoryEvent event) {
            if (event == null || !IMPORT_STARTED_UEI.equals(event.getUei())) {
                return;
            }

            // Extract the name of the referenced resource
            final String actualResource = event.getParameterValue(PARM_IMPORT_RESOURCE).orElse(null);
            if (actualResource == null) {
                // ignore
                return;
            }

            if (actualResource.contains(sessionId)) {
                startedLatch.countDown();
            }
        }

        public void waitForImportStarted() throws InterruptedException {
            startedLatch.await();
        }

        @Override
        public void close() {
            eventSubscriptionService.removeEventListener(this, IMPORT_STARTED_UEI);
        }
    }

}
