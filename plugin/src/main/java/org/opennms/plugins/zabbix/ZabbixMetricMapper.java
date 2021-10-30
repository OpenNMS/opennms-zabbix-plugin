package org.opennms.plugins.zabbix;

import static org.opennms.plugins.zabbix.model.PreprocessingRule.CHANGE_PER_SECOND;

import org.opennms.integration.api.v1.collectors.immutables.ImmutableNumericAttribute;
import org.opennms.integration.api.v1.collectors.immutables.ImmutableStringAttribute;
import org.opennms.integration.api.v1.collectors.resource.NumericAttribute;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSetResource;
import org.opennms.plugins.zabbix.expressions.Constant;
import org.opennms.plugins.zabbix.expressions.ExpressionParser;
import org.opennms.plugins.zabbix.expressions.ItemKey;
import org.opennms.plugins.zabbix.expressions.ParseException;
import org.opennms.plugins.zabbix.expressions.Term;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Item;

import com.google.common.base.Strings;

public class ZabbixMetricMapper {
    public static final String DEFAULT_GROUP_NAME = "zabbix";
    private final ExpressionParser expressionParser = new ExpressionParser();

    public NumericAttribute.Type getMetricType(Item item) {
        boolean isChangePerSecond = item.getPreprocessing().stream()
                .anyMatch(rule -> CHANGE_PER_SECOND.equals(rule.getType()));
        if (isChangePerSecond) {
            return NumericAttribute.Type.COUNTER;
        }
        return NumericAttribute.Type.GAUGE;
    }

    public String getMetricName(ItemKey key) {
        // no parameters - use the key directly
        if (key.getParameters().size() < 1) {
            return key.getName();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(key.getName());
        for (Term term : key.getParameters()) {
            if (!(term instanceof Constant)) {
                throw new RuntimeException("Non constant parameters are currently supported! Got: " + term);
            }
            final String parmValue = ((Constant) term).getValue();
            // Skip empty parameters
            if (Strings.isNullOrEmpty(parmValue)) {
                continue;
            }
            // Skip parameters with macros
            if (ZabbixMacroSupport.containsMacro(parmValue)) {
                continue;
            }
            sb.append(".");
            sb.append(parmValue);
        }
        return sb.toString();
    }

    public String getMetricName(String key) {
        synchronized (expressionParser) {
            try {
                return getMetricName(expressionParser.parseItem(key));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addValueToResource(Item item, String value, ImmutableCollectionSetResource.Builder<?> resourceBuilder) {
        addValueToResource(null, item, value, resourceBuilder);
    }

    public void addValueToResource(DiscoveryRule rule, Item item, String value, ImmutableCollectionSetResource.Builder<?> resourceBuilder) {
        if (value == null) {
            // ignore null values
            return;
        }

        final String name = getMetricName(item.getKey());
        double numericValue = Double.NaN;
        NumericAttribute.Type numericType = null;
        try {
            numericValue = Double.parseDouble(value);
            numericType = getMetricType(item);
        } catch (NumberFormatException e) {
            // pass
        }

        String groupName = DEFAULT_GROUP_NAME;
        if (rule != null) {
            groupName = rule.getKey();
        }

        if (numericType != null) {
            resourceBuilder.addNumericAttribute(ImmutableNumericAttribute.newBuilder()
                    .setGroup(groupName)
                    .setName(name)
                    .setType(numericType)
                    .setValue(numericValue)
                    .build());
        } else {
            resourceBuilder.addStringAttribute(ImmutableStringAttribute.newBuilder()
                    .setGroup(groupName).setName(name).setValue(value).build());
        }
    }


}
