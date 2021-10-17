package org.opennms.plugins.zabbix;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.opennms.integration.api.xml.schema.datacollection.Parameter;
import org.opennms.integration.api.xml.schema.datacollection.PersistenceSelectorStrategy;
import org.opennms.integration.api.xml.schema.datacollection.ResourceType;
import org.opennms.integration.api.xml.schema.datacollection.StorageStrategy;
import org.opennms.plugins.zabbix.model.Condition;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Filter;
import org.opennms.plugins.zabbix.model.ItemPrototype;
import org.opennms.plugins.zabbix.model.Macro;
import org.opennms.plugins.zabbix.model.Tag;

import com.google.common.base.Strings;

public class ZabbixResourceTypeGenerator {

    public static final String REGEX_STRAT_CLASS = "org.opennms.netmgt.collectd.PersistRegexSelectorStrategy";
    public static final String PERSIST_ALL_STRAT_CLASS = "org.opennms.netmgt.collection.support.PersistAllSelectorStrategy";
    public static final String SIBLING_STRAT_CLASS = "org.opennms.netmgt.dao.support.SiblingColumnStorageStrategy";

    public org.opennms.integration.api.v1.config.datacollection.ResourceType getResourceTypeFor( DiscoveryRule rule) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(rule.getKey());
        resourceType.setLabel(rule.getName());
        resourceType.setResourceLabel(getFirstTag(rule).map(t -> macrosToVariables(t.getValue()))
                .orElse("${index}"));
        resourceType.setPersistenceSelectorStrategy(getPersistenceSelectorStrategy(rule));
        resourceType.setStorageStrategy(getStorageStrategy(rule));
        return resourceType;
    }

    private PersistenceSelectorStrategy getPersistenceSelectorStrategy(DiscoveryRule rule) {
        final Filter filter = rule.getFilter();
        if (filter == null) {
            PersistenceSelectorStrategy persistenceSelectorStrategy = new PersistenceSelectorStrategy();
            persistenceSelectorStrategy.setClazz(PERSIST_ALL_STRAT_CLASS);
            return persistenceSelectorStrategy;
        }

        if (!"AND".equals(filter.getEvalType())) {
            throw new RuntimeException("Unsupported filter for rule " + rule.getName());
        }

        PersistenceSelectorStrategy persistenceSelectorStrategy = new PersistenceSelectorStrategy();
        persistenceSelectorStrategy.setClazz(REGEX_STRAT_CLASS);
        List<org.opennms.integration.api.v1.config.datacollection.Parameter> parameters = new LinkedList<>();
        for (Condition condition : filter.getConditions()) {
            Parameter parameter = new Parameter();
            parameter.setKey("match-expression");

            String operator;
            if (condition.getOperator() == null || "MATCHES_REGEX".equals(condition.getOperator())) {
                operator = "matches";
            } else if ("NOT_MATCHES_REGEX".equals(condition.getOperator())) {
                operator = "not matches";
            } else {
                throw new RuntimeException("Unsupported operator for rule: " + rule.getName() + " - " + condition.getOperator() );
            }

            final String varName = getVariableNameFromMacro(condition.getMacro());
            if (varName == null) {
                throw new RuntimeException("Unsupported macro for rule: " + rule.getName() + " - " + condition.getMacro());
            }

            final String pattern = evaluateMacro(condition.getValue(), rule.getTemplate().getMacros());

            // SPeL expression - all non-numeric attributes are part of the context
            parameter.setValue(String.format("(#%s %s '%s')", varName, operator, pattern));
            parameters.add(parameter);
        }
        persistenceSelectorStrategy.setParameters(parameters);
        return persistenceSelectorStrategy;
    }

    private StorageStrategy getStorageStrategy(DiscoveryRule rule) {
        // Determine a unique index to use
        // We search through the item prototype to find the referenced macros and use these as the column name
        final Set<String> macros = rule.getItemPrototypes().stream()
                .map(ItemPrototype::getKey)
                .flatMap(key -> getMacros(key).stream())
                .collect(Collectors.toSet());
        if (macros.size() != 1) {
            throw new RuntimeException("FIXME: Add support for many. Too many macros found for rule: " + rule.getName() + ": " + macros);
        }
        final String macro = macros.stream().findFirst().get();
        final String varName = getVariableNameFromMacro(macro);
        if (varName == null) {
            throw new RuntimeException("FIXME: Unsupported macro: " + macro);
        }

        StorageStrategy storageStrategy = new StorageStrategy();
        storageStrategy.setClazz(SIBLING_STRAT_CLASS);
        List<org.opennms.integration.api.v1.config.datacollection.Parameter> parameters = new LinkedList<>();
        Parameter parameter = new Parameter();
        parameter.setKey("sibling-column-name");
        parameter.setValue(varName);
        parameters.add(parameter);
        storageStrategy.setParameters(parameters);
        return storageStrategy;
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

    public static List<String> getMacros(String source) {
        if (Strings.isNullOrEmpty(source)) {
            // nothing to do
            return Collections.emptyList();
        }

        final List<String> macros = new LinkedList<>();
        final Matcher m = MACRO_FIND_PATTERN.matcher(source);
        while (m.find()) {
            macros.add(m.group(1));
        }
        return macros;
    }

    public static String getVariableNameFromMacro(String macro) {
        final Matcher m = MACRO_FIND_PATTERN.matcher(macro);
        if (!m.matches()) {
            return null;
        }
        return m.group(2);
    }

    public static String evaluateMacro(String value, List<Macro> macros) {
        for (Macro macro : macros) {
            if (Objects.equals(value, macro.getMacro())) {
                return macro.getValue();
            }
        }
        return value;
    }

}
