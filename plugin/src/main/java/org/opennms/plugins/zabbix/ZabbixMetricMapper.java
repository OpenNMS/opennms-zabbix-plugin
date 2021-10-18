package org.opennms.plugins.zabbix;

import static org.opennms.plugins.zabbix.model.PreprocessingRule.CHANGE_PER_SECOND;

import org.opennms.integration.api.v1.collectors.immutables.ImmutableNumericAttribute;
import org.opennms.integration.api.v1.collectors.immutables.ImmutableStringAttribute;
import org.opennms.integration.api.v1.collectors.resource.NumericAttribute;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSetResource;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Item;
import org.opennms.plugins.zabbix.model.ZabbixKey;

import com.google.common.base.Strings;

public class ZabbixMetricMapper {
    public static final String DEFAULT_GROUP_NAME = "zabbix";

    public NumericAttribute.Type getMetricType(Item item) {
        boolean isChangePerSecond = item.getPreprocessing().stream()
                .anyMatch(rule -> CHANGE_PER_SECOND.equals(rule.getType()));
        if (isChangePerSecond) {
            return NumericAttribute.Type.COUNTER;
        }
        return NumericAttribute.Type.GAUGE;
    }

    public String getMetricName(String key) {
        // parse the key
        final ZabbixKey zabbixKey = new ZabbixKey(key);
        // no parameters - use the key directly
        if (zabbixKey.getParamterCount() < 1) {
            return zabbixKey.getName();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(zabbixKey.getName());
        for (String parameter : zabbixKey.getParameters()) {
            // Skip empty parameters
            if (Strings.isNullOrEmpty(parameter)) {
                continue;
            }
            // Skip parameters with macros
            if (ZabbixMacroSupport.containsMacro(parameter)) {
                continue;
            }
            sb.append(".");
            sb.append(parameter);
        }
        return sb.toString();
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
