package org.opennms.plugins.zabbix.lab;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;

import org.opennms.integration.api.v1.config.requisition.Requisition;
import org.opennms.integration.api.v1.config.requisition.SnmpPrimaryType;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisition;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionInterface;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionMetaData;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionNode;
import org.opennms.integration.api.v1.requisition.RequisitionProvider;
import org.opennms.integration.api.v1.requisition.RequisitionRequest;

public class LabRequisitionProvider implements RequisitionProvider {
    public static final String TYPE = "zabbix-lab";

    private final LabContextManager labContextManager;

    public LabRequisitionProvider(LabContextManager labContextManager) {
        this.labContextManager = Objects.requireNonNull(labContextManager);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequisitionRequest getRequest(Map<String, String> parameters) {
        return new MyRequisitonRequest(parameters);
    }

    @Override
    public Requisition getRequisition(RequisitionRequest genericRequest) {
        final MyRequisitonRequest request = (MyRequisitonRequest) genericRequest;
        labContextManager.trackGetRequisitionForSession(request.getSessionId());
        return ImmutableRequisition.newBuilder()
                .setForeignSource(request.getForeignSource())
                .addNode(ImmutableRequisitionNode.newBuilder()
                        .setForeignId("mock-agent-" + request.getSessionId())
                        .setNodeLabel("mock-agent-" + request.getSessionId())
                        .addMetaData(ImmutableRequisitionMetaData.newBuilder()
                                .setContext("zabbixLab")
                                .setKey("sessionId")
                                .setValue(request.getSessionId())
                                .build())
                        .addInterface(ImmutableRequisitionInterface.newBuilder()
                                .setIpAddress(request.getAgentIp())
                                .setSnmpPrimary(SnmpPrimaryType.NOT_ELIGIBLE)
                                .build())
                        .build())
                .build();
    }

    @Override
    public byte[] marshalRequest(RequisitionRequest request) {
        throw new UnsupportedOperationException("No Minion support.");
    }

    @Override
    public RequisitionRequest unmarshalRequest(byte[] bytes) {
        throw new UnsupportedOperationException("No Minion support.");
    }

    private static class MyRequisitonRequest implements RequisitionRequest {
        private final Map<String, String> parameters;

        public MyRequisitonRequest(Map<String, String> parameters) {
            this.parameters = Objects.requireNonNull(parameters);
        }

        public String getForeignSource() {
            return parameters.getOrDefault("foreignSource", "fs");
        }

        public String getSessionId() {
            return parameters.get(LabContextManager.SESSION_ID_PARM_NAME);
        }

        public InetAddress getAgentIp() {
            try {
                return InetAddress.getByName(parameters.get("agentIp"));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
