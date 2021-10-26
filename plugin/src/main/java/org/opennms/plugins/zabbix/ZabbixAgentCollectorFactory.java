package org.opennms.plugins.zabbix;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.opennms.integration.api.v1.collectors.CollectionRequest;
import org.opennms.integration.api.v1.collectors.ServiceCollectorFactory;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.model.Node;
import org.opennms.plugins.zabbix.model.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ZabbixAgentCollectorFactory implements ServiceCollectorFactory<ZabbixAgentCollector> {
    private static final Logger LOG = LoggerFactory.getLogger(ZabbixAgentCollectorFactory.class);

    public static final String NODE_ID_KEY = "nodeId";
    public static final String TEMPLATES_KEY = "templates";

    private final NodeDao nodeDao;
    private final TemplateResolver templateResolver;
    private final ObjectMapper om;
    private InetAddress agentAddress;
    private int agentPort;

    public ZabbixAgentCollectorFactory(NodeDao nodeDao, TemplateResolver templateResolver) {
        this.nodeDao = Objects.requireNonNull(nodeDao);
        this.templateResolver = Objects.requireNonNull(templateResolver);

        om = new ObjectMapper(new YAMLFactory());
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void setAddressAndPort(InetAddress address, int port){
        agentAddress = address;
        agentPort = port;
    }

    @Override
    public ZabbixAgentCollector createCollector() {
        if(agentAddress == null) {
            throw new ZabbixNotSupportedException("Agent address not set");
        }
        ZabbixAgentClient client = new ZabbixAgentClient(agentAddress, agentPort);
        return new ZabbixAgentCollector(client);
    }

    @Override
    public String getCollectorClassName() {
        return ZabbixAgentCollector.class.getCanonicalName();
    }

    @Override
    public Map<String, Object> getRuntimeAttributes(CollectionRequest collectionRequest) {
        final Map<String, Object> attributes = new LinkedHashMap<>();

        int nodeId = 0;
        final Node node = nodeDao.getNodeByCriteria(collectionRequest.getNodeCriteria());
        if (node != null) {
            nodeId = node.getId();
        }
        attributes.put(NODE_ID_KEY, Integer.toString(nodeId));

        // Find the templates for the node
        final List<Template> templates = templateResolver.getTemplatesForNode(collectionRequest.getNodeCriteria());
        attributes.put(TEMPLATES_KEY, templates);
        return attributes;
    }

    @Override
    public Map<String, String> marshalParameters(Map<String, Object> map) {
        final Map<String,String> marshaledMap = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (TEMPLATES_KEY.equals(key)) {
                try {
                    marshaledMap.put(key, om.writeValueAsString(value));
                } catch (JsonProcessingException ex) {
                    LOG.error("Failed to marshal templates to JSON.", ex);
                }
            } else {
                marshaledMap.put(key, value.toString());
            }
        });
        return marshaledMap;
    }

    @Override
    public Map<String, Object> unmarshalParameters(Map<String, String> map) {
        final Map<String,Object> unmarshaledMap = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (TEMPLATES_KEY.equals(key)) {
                try {
                    final Template[] templates = om.readValue(value, Template[].class);
                    unmarshaledMap.put(key, Arrays.asList(templates));
                } catch (JsonProcessingException ex) {
                    LOG.error("Failed to unmarshal templates from JSON.", ex);
                }
            } else {
                unmarshaledMap.put(key, value);
            }
        });
        return unmarshaledMap;
    }
}
