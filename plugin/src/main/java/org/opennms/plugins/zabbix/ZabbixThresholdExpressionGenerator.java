package org.opennms.plugins.zabbix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.opennms.integration.api.v1.config.thresholding.Expression;
import org.opennms.integration.api.v1.config.thresholding.FilterOperator;
import org.opennms.integration.api.v1.config.thresholding.ResourceFilter;
import org.opennms.integration.api.v1.config.thresholding.ThresholdType;
import org.opennms.plugins.zabbix.expressions.Array;
import org.opennms.plugins.zabbix.expressions.Constant;
import org.opennms.plugins.zabbix.expressions.ExpressionParser;
import org.opennms.plugins.zabbix.expressions.FunctionCall;
import org.opennms.plugins.zabbix.expressions.HostAndKey;
import org.opennms.plugins.zabbix.expressions.ItemKey;
import org.opennms.plugins.zabbix.expressions.Operator;
import org.opennms.plugins.zabbix.expressions.ParseException;
import org.opennms.plugins.zabbix.expressions.Term;
import org.opennms.plugins.zabbix.expressions.TermVisitor;
import org.opennms.plugins.zabbix.model.Macro;
import org.opennms.plugins.zabbix.model.Trigger;

public class ZabbixThresholdExpressionGenerator {

    private final ZabbixTemplateHandler zabbixTemplateHandler;
    private final ExpressionParser expressionParser = new ExpressionParser();

    public ZabbixThresholdExpressionGenerator(ZabbixTemplateHandler zabbixTemplateHandler) {
        this.zabbixTemplateHandler = Objects.requireNonNull(zabbixTemplateHandler);
    }

    public List<Expression> getThresholdingExpressions() {
        List<Expression> expressions = new LinkedList<>();
        // items
        zabbixTemplateHandler.getTemplates().stream()
                .flatMap(t -> t.getItems().stream())
                .flatMap(r -> r.getTriggers().stream())
                .map(this::tryToParse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(expressions::add);
        zabbixTemplateHandler.getTemplates().stream()
                .flatMap(t -> t.getDiscoveryRules().stream())
                .flatMap(r -> r.getTriggerPrototypes().stream())
                .map(this::tryToParse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(expressions::add);
        return expressions;
    }

    /**
     * Expression: min(/h/k[{$MACRO}],15m) < {$MACRO} or min(/h/kk[${MACRO},15m)) > {$MACRO}
     * Current parser, parses out in the following tree:
     *                           Expression
     *          lhs=Expression                                   operator=>           rhs=Constant {$MACRO}
     *     lhs=Expression              op=or  rhs=FnCall
     *  lhs=FnCall op=< rhs=Constant
     */
    private static class ExpressionAccumulator implements TermVisitor {
        private List<Term> stack = new ArrayList<>();

        public void accumulate(org.opennms.plugins.zabbix.expressions.Expression expression) {
            expression.visit(this);
        }

        @Override
        public void visitExpression(org.opennms.plugins.zabbix.expressions.Expression expression) {
            expression.getLhs().visit(this);
            expression.getOperator().visit(this);
            expression.getRhs().visit(this);
        }

        @Override
        public void visitArray(Array array) {
            throw new ExpressionNotSupportedException("arrays are not supported.");
        }

        @Override
        public void visitConstant(Constant constant) {
            stack.add(constant);
        }

        @Override
        public void visitFunctionCall(FunctionCall fn) {
            if (!"max".equals(fn.getName()) && !"min".equals(fn.getName()) && !"last".equals(fn.getName())) {
                throw new ExpressionNotSupportedException("Unsupported function call: " + fn.getName());
            }
            if (fn.getParameters().size() != 2) {
                throw new ExpressionNotSupportedException("Unsupported # parameters for function call: " + fn.getParameters().size());
            }
            if (!(fn.getParameters().get(0) instanceof HostAndKey)) {
                throw new ExpressionNotSupportedException("first parm must be host and key, but got: " + fn.getParameters().get(0));
            }
            if (!(fn.getParameters().get(1) instanceof ItemKey)) {
                throw new ExpressionNotSupportedException("2nd parm must be key, but got: " + fn.getParameters().get(1));
            }

            HostAndKey hostAndKey = (HostAndKey)fn.getParameters().get(0);
            stack.add(hostAndKey.getKey());

            // FIXME: Should be able to derive the 'trigger' value from this
            //ItemKey maxKey = (ItemKey)fn.getParameters().get(1);
        }

        @Override
        public void visitHostAndKey(HostAndKey hostAndKey) {
            stack.add(hostAndKey);
        }

        @Override
        public void visitItemKey(ItemKey itemKey) {
            stack.add(itemKey);
        }

        @Override
        public void visitOperator(Operator operator) {
            stack.add(operator);
        }

        public List<Term> getStack() {
            return stack;
        }
    }

    public Optional<ZabbixThresholdingExpression> tryToParse(Trigger trigger) {
        try {
            return Optional.of(parse(trigger));
        } catch (ExpressionNotSupportedException e) {
            return Optional.empty();
        }
    }

    private String getExpression(List<Term> stack, int offset, List<Macro> macros) {
        final ItemKey key;
        final Operator op;
        final Constant constant;

        try {
            key = (ItemKey)stack.get(offset);
            op = (Operator)stack.get(1 + offset);
            constant = (Constant)stack.get(2 + offset);
        } catch (ClassCastException c) {
            throw new ExpressionNotSupportedException("unexpected attributes on stack: " + stack, c);
        }

        // rhsConstant is {$MEM.PAGE_TABLE_CRIT.MIN}
        // defined in macro with a default value of 5000 on the 'Windows memory by Zabbix agent' template
        double value = Double.NaN;
        try {
            value = Double.parseDouble(ZabbixMacroSupport.evaluateMacro(constant.getValue(), macros));
        } catch (NumberFormatException e) {
            throw new ExpressionNotSupportedException("constant does not evaluate to double: "
                    + constant.getValue() + " with macros: " + macros, e);
        }

        ZabbixMetricMapper zabbixMetricMapper = new ZabbixMetricMapper();
        String metricName = zabbixMetricMapper.getMetricName(key);

        return String.format("(datasources['%s'] %s %.2f) ? 1 : 0", metricName, op.getValue(), value);
    }

    public ZabbixThresholdingExpression parse(Trigger trigger) {
        // Start by parsing the expression
        final org.opennms.plugins.zabbix.expressions.Expression parsedExpression;
        try {
            parsedExpression = expressionParser.parse(trigger.getExpression());
        } catch (ParseException e) {
            throw new ExpressionNotSupportedException("Failed to parse expression: " + trigger.getExpression(), e);
        }

        // Gather the macros for replacement
        List<Macro> macrosInTemplate;
        if (trigger.getItem() != null) {
            macrosInTemplate = trigger.getItem().getTemplate().getMacros();
        } else if (trigger.getDiscoveryRule() != null) {
            macrosInTemplate = trigger.getDiscoveryRule().getTemplate().getMacros();
        } else {
            throw new RuntimeException("Expecting parent item or rule to be present on trigger: " + trigger);
        }

        ExpressionAccumulator accumulator = new ExpressionAccumulator();
        accumulator.accumulate(parsedExpression);
        if (accumulator.getStack().size() != 3 && accumulator.getStack().size() != 7)  {
            throw new UnsupportedOperationException("Unsupported stack: " + accumulator.getStack());
        }

        String expression;
        if (accumulator.getStack().size() == 3) {
            // Simple expression
            expression = getExpression(accumulator.getStack(), 0, macrosInTemplate);
        } else {
            expression = String.format("(%s) + (%s)",
                    getExpression(accumulator.getStack(), 0, macrosInTemplate),
                    getExpression(accumulator.getStack(), 4, macrosInTemplate));
        }

        return new ZabbixThresholdingExpression(trigger, expression);
    }

    public static class ZabbixThresholdingExpression implements Expression {
        private final Trigger trigger;
        private final String expression;
        private final ZabbixEventGenerator zabbixEventGenerator = new ZabbixEventGenerator();

        public ZabbixThresholdingExpression(Trigger trigger, String expression) {
            this.trigger = Objects.requireNonNull(trigger);
            this.expression = Objects.requireNonNull(expression);
        }

        public Trigger getZabbixTrigger() {
            return trigger;
        }

        @Override
        public String getExpression() {
            return expression;
        }

        @Override
        public Boolean getRelaxed() {
            return true;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.of(trigger.getDescription());
        }

        @Override
        public ThresholdType getType() {
            return ThresholdType.HIGH;
        }

        @Override
        public String getDsType() {
            if (trigger.getDiscoveryRule() != null) {
                return ZabbixResourceTypeGenerator.sanitizeResourceName(trigger.getDiscoveryRule().getKey());
            }
            return "node";
        }

        @Override
        public Double getValue() {
            return 1.0d;
        }

        @Override
        public Double getRearm() {
           return 0.0d;
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
            return Optional.of(zabbixEventGenerator.getTriggerEventDefinition(trigger).getUei());
        }

        @Override
        public Optional<String> getRearmedUEI() {
            return Optional.of(zabbixEventGenerator.getRearmEventDefinition(trigger).getUei());
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


    public static class ExpressionNotSupportedException extends RuntimeException {
        public ExpressionNotSupportedException(String message) {
            super(message);
        }

        public ExpressionNotSupportedException(String message, Throwable cause) {
            super(message, cause);
        }

    }
}
