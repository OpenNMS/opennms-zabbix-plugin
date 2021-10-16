package org.opennms.plugins.zabbix;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opennms.integration.api.xml.schema.datacollection.ResourceType;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.ItemPrototype;
import org.opennms.plugins.zabbix.model.Tag;

import com.google.common.base.Strings;

public class ZabbixResourceTypeGenerator {

    public org.opennms.integration.api.v1.config.datacollection.ResourceType getResourceTypeFor(DiscoveryRule rule) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(rule.getKey());
        resourceType.setLabel(rule.getName());
        resourceType.setResourceLabel(getFirstTag(rule).map(t -> macrosToVariables(t.getValue()))
                .orElse("${index}"));
        return resourceType;
    }

    /**
     * Retrieve the first available tag from a discovery rule.
     */
    private Optional<Tag> getFirstTag(DiscoveryRule rule) {
        for (ItemPrototype prototype : rule.getItemPrototypes()) {
            for (Tag tag : prototype.getTags()) {
                return Optional.of(tag);
            }
        }
        return Optional.empty();
    }

    private static final Pattern MACRO_FIND_PATTERN = Pattern.compile("(\\{#([^{]*?)\\})");


    /**
     * Convert macros to variable placeholder
     */
    public static String macrosToVariables(String source) {
        if (Strings.isNullOrEmpty(source)) {
            // nothing to replace
            return source;
        }

        final Matcher m = MACRO_FIND_PATTERN.matcher(source);
        final StringBuilder sb = new StringBuilder();
        int offset = 0;

        while (m.find()) {
            // copy characters between offset and start
            sb.append(source, offset, m.start());
            // replace
            sb.append("${");
            sb.append(m.group(2));
            sb.append("}");
            offset = m.end();
        }
        // copy remaining characters
        sb.append(source, offset, source.length());
        return sb.toString();
    }

}
