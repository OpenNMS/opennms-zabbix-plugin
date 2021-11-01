package org.opennms.plugins.zabbix;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opennms.plugins.zabbix.model.Macro;

import com.google.common.base.Strings;

/**
 * Misc. functions for dealing with Zabbix macros.
 *
 * Quick macro reference:
 *   General: {MACRO}
 *   User macro: {$MACRO}
 *   Low level discovery macro: {#MACRO}
 *   Macros with context: {$MACRO:"static text"}
 *   Macros with regular expressions: {$MACRO:regex:"regular expression"}
 *   Macros with functions: {{#IFALIAS}.regsub("(.*)_([0-9]+)", \1)}
 *
 */
public class ZabbixMacroSupport {

    private static final Pattern GENERIC_MACRO_PATTERN = Pattern.compile("\\{(.+)}");

    private static final Pattern MACRO_FIND_PATTERN = Pattern.compile("(\\{#([^{]*?)\\})");

    public static boolean containsMacro(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }
        return GENERIC_MACRO_PATTERN.matcher(value).find();
    }

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
        final LinkedHashMap<String, Object> macroMap = new LinkedHashMap<>();
        for (Macro macro : macros) {
            macroMap.put(macro.getMacro(), macro.getValue());
        }
        return evaluateMacro(value, macroMap);
    }

    public static String evaluateMacro(String value, Map<String, Object> entry) {
        if (Strings.isNullOrEmpty(value)) {
            // nothing to replace
            return value;
        }

        // FIXME: HACKZ: {$VFS.DEV.READ.AWAIT.WARN:"{#DEVNAME}"} -> {$VFS.DEV.READ.AWAIT.WARN}
        value = value.replaceAll(":\\\"\\{#.+}\\\"", "");

        final Matcher m = GENERIC_MACRO_PATTERN.matcher(value);
        final StringBuilder sb = new StringBuilder();
        int offset = 0;

        while (m.find()) {
            // copy characters between offset and start
            sb.append(value, offset, m.start());

            final String innerMacro = m.group(1);
            final String outerMacro = m.group(0);
            if (innerMacro.startsWith("#")) {
                // replace
                if (entry.containsKey(innerMacro)) {
                    sb.append(entry.get(innerMacro));
                } else {
                    sb.append(entry.getOrDefault(outerMacro, ""));
                }
            } else {
                sb.append(entry.getOrDefault(outerMacro, ""));
            }
            offset = m.end();
        }

        // copy remaining characters
        sb.append(value, offset, value.length());
        return sb.toString();
    }

}
