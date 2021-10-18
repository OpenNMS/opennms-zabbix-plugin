package org.opennms.plugins.zabbix.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsing of Zabbix keys.
 *
 * See https://www.zabbix.com/documentation/current/manual/config/items/item/key for reference
 */
public class ZabbixKey {

    final Pattern KEY_PATTERN = Pattern.compile("([0-9a-zA-Z_\\-.]+)(\\[(.*)])?");

    private final String key;
    private final String name;
    private final List<String> parameters;

    public ZabbixKey(String key) {
        this.key = Objects.requireNonNull(key);
        final Matcher m = KEY_PATTERN.matcher(key);
        if (!m.matches()) {
            throw new RuntimeException("invalid key: " + key);
        }
        name = m.group(1);
        if (m.group(3) != null) {
            parameters = Arrays.asList(m.group(3).split(","));
        } else {
            parameters = Collections.emptyList();
        }
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public int getParamterCount() {
        return parameters.size();
    }

    public List<String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZabbixKey zabbixKey = (ZabbixKey) o;
        return Objects.equals(key, zabbixKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
