package org.opennms.plugins.zabbix;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opennms.plugins.zabbix.model.Macro;

import com.google.common.base.Strings;

/**
 * Misc. functions for dealing with Zabbix macros.
 */
public class ZabbixMacroSupport {
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

    public static String evaluateMacro(String value, Map<String, Object> entry) {
        if (Strings.isNullOrEmpty(value)) {
            // nothing to replace
            return value;
        }

        final Matcher m = MACRO_FIND_PATTERN.matcher(value);
        final StringBuilder sb = new StringBuilder();
        int offset = 0;

        while (m.find()) {
            // copy characters between offset and start
            sb.append(value, offset, m.start());
            // replace
            sb.append(entry.getOrDefault(m.group(1), ""));
            offset = m.end();
        }
        // copy remaining characters
        sb.append(value, offset, value.length());
        return sb.toString();
    }
}
