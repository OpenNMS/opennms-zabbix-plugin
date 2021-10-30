package org.opennms.plugins.zabbix.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.opennms.integration.api.v1.config.thresholding.Expression;
import org.opennms.integration.api.v1.config.thresholding.GroupDefinition;
import org.opennms.integration.api.v1.config.thresholding.Threshold;
import org.opennms.plugins.zabbix.ZabbixTemplateHandler;
import org.opennms.plugins.zabbix.ZabbixThresholdExpressionGenerator;

public class ThresholdingConfigExtension implements org.opennms.integration.api.v1.config.thresholding.ThresholdingConfigExtension {

    private final ZabbixTemplateHandler zabbixTemplateHandler;

    public ThresholdingConfigExtension(ZabbixTemplateHandler zabbixTemplateHandler) {
        this.zabbixTemplateHandler = Objects.requireNonNull(zabbixTemplateHandler);
    }

    @Override
    public List<GroupDefinition> getGroupDefinitions() {
        final ZabbixThresholdExpressionGenerator zabbixThresholdExpressionGenerator = new ZabbixThresholdExpressionGenerator(zabbixTemplateHandler);
        final ZabbixGroupDefinition zabbixGroupDefinition = new ZabbixGroupDefinition(zabbixThresholdExpressionGenerator.getThresholdingExpressions());
        return Collections.singletonList(zabbixGroupDefinition);
    }


    public static class ZabbixGroupDefinition implements GroupDefinition {
        private final List<Expression> expressions;

        public ZabbixGroupDefinition(List<Expression> expressions) {
            this.expressions = Objects.requireNonNull(expressions);
        }

        @Override
        public String getName() {
            return "Zabbix";
        }

        @Override
        public String getRrdRepository() {
            return "/opt/opennms/share/rrd/snmp/";
        }

        @Override
        public List<Threshold> getThresholds() {
            return Collections.emptyList();
        }

        @Override
        public List<Expression> getExpressions() {
            return expressions;
        }
    }

}
