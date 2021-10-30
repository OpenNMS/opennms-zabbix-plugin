package org.opennms.plugins.zabbix;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opennms.integration.api.v1.config.thresholding.Expression;
import org.opennms.integration.api.v1.config.thresholding.FilterOperator;
import org.opennms.integration.api.v1.config.thresholding.ResourceFilter;
import org.opennms.integration.api.v1.config.thresholding.ThresholdType;
import org.opennms.plugins.zabbix.expressions.Constant;
import org.opennms.plugins.zabbix.expressions.ExpressionParser;
import org.opennms.plugins.zabbix.expressions.FunctionCall;
import org.opennms.plugins.zabbix.expressions.HostAndKey;
import org.opennms.plugins.zabbix.expressions.ItemKey;
import org.opennms.plugins.zabbix.expressions.ParseException;
import org.opennms.plugins.zabbix.model.Macro;
import org.opennms.plugins.zabbix.model.Trigger;

public class ZabbixThresholdExpressionGenerator {

    private final ZabbixTemplateHandler zabbixTemplateHandler;
    private final ExpressionParser expressionParser = new ExpressionParser();

    public ZabbixThresholdExpressionGenerator(ZabbixTemplateHandler zabbixTemplateHandler) {
        this.zabbixTemplateHandler = Objects.requireNonNull(zabbixTemplateHandler);
    }

    public List<Expression> getThresholdingExpressions() {
        return zabbixTemplateHandler.getTemplates().stream()
                .flatMap(t -> t.getItems().stream())
                .flatMap(r -> r.getTriggers().stream())
                .map(this::tryToParse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Optional<ZabbixThresholdingExpression> tryToParse(Trigger trigger) {
        // Start by parsing the expression
        final org.opennms.plugins.zabbix.expressions.Expression parsedExpression;
        try {
            parsedExpression = expressionParser.parse(trigger.getExpression());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        if (!(parsedExpression.getLhs() instanceof FunctionCall) ||
                !(parsedExpression.getRhs() instanceof Constant)) {
            return Optional.empty();
        }
        if (!"<".equals(parsedExpression.getOperator()) && !">".equals(parsedExpression.getOperator())) {
            return Optional.empty();
        }
        final FunctionCall fn = (FunctionCall)parsedExpression.getLhs();
        if (!"max".equals(fn.getName())) {
            return Optional.empty();
        }
        if (fn.getParameters().size() != 2) {
            return Optional.empty();
        }
        if (!(fn.getParameters().get(0) instanceof HostAndKey)) {
            return Optional.empty();
        }
        if (!(fn.getParameters().get(1) instanceof ItemKey)) {
            return Optional.empty();
        }

        HostAndKey hostAndKey = (HostAndKey)fn.getParameters().get(0);
        if (hostAndKey.getKey().getParameters().size() != 1) {
            return Optional.empty();
        }

        // FIXME: Should be able to derive the 'trigger' value from this
        ItemKey maxKey = (ItemKey)fn.getParameters().get(1);
        final Constant rhsConstant = (Constant)parsedExpression.getRhs();

        final ThresholdType type;
        if ("<".equals(parsedExpression.getOperator())) {
            type = ThresholdType.LOW;
        } else {
            type = ThresholdType.HIGH;
        }

        // rhsConstant is {$MEM.PAGE_TABLE_CRIT.MIN}
        // defined in macro with a default value of 5000 on the 'Windows memory by Zabbix agent' template
        double value = Double.NaN;
        List<Macro> macrosInTemplate = trigger.getItem().getTemplate().getMacros();
        for (Macro macro : macrosInTemplate) {
            if (rhsConstant.getValue().equals(macro.getMacro())) {
                value = Double.parseDouble(macro.getValue());
            }
        }

        ItemKey key = hostAndKey.getKey();
        ZabbixMetricMapper zabbixMetricMapper = new ZabbixMetricMapper();
        String metricName = zabbixMetricMapper.getMetricName(key);

        return Optional.of(new ZabbixThresholdingExpression(trigger, metricName, type, value));
    }

    public static class ZabbixThresholdingExpression implements Expression {
        private final Trigger trigger;
        private final String metricName;
        private final ThresholdType type;
        private final double value;

        public ZabbixThresholdingExpression(Trigger trigger, String metricName, ThresholdType type, double value) {
            this.trigger = Objects.requireNonNull(trigger);
            this.metricName = Objects.requireNonNull(metricName);
            this.type = Objects.requireNonNull(type);
            this.value = value;
        }

        public Trigger getZabbixTrigger() {
            return trigger;
        }

        @Override
        public String getExpression() {
            return metricName;
        }

        @Override
        public Boolean getRelaxed() {
            return true;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.empty();
        }

        @Override
        public ThresholdType getType() {
            return type;
        }

        @Override
        public String getDsType() {
            return "node"; // FIXME: other resource types?
        }

        @Override
        public Double getValue() {
            return value;
        }

        @Override
        public Double getRearm() {
            // FIXME: Hackz
            if (type == ThresholdType.HIGH) {
                return value - 1;
            } else {
                return value + 1;
            }
        }

        @Override
        public Integer getTrigger() {
            return 1;
        }

        @Override
        public Optional<String> getDsLabel() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getTriggeredUEI() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getRearmedUEI() {
            return Optional.empty();
        }

        @Override
        public FilterOperator getFilterOperator() {
            return FilterOperator.OR;
        }

        @Override
        public List<ResourceFilter> getResourceFilters() {
            return Collections.emptyList();
        }
    }

}
