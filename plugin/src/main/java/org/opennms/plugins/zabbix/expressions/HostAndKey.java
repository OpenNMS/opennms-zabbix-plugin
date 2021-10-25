package org.opennms.plugins.zabbix.expressions;

import java.util.Objects;
import java.util.StringJoiner;

public class HostAndKey implements Term {
    private final String host;
    private final ItemKey key;

    public HostAndKey(String host, ItemKey key) {
        this.host = Objects.requireNonNull(host);
        this.key = Objects.requireNonNull(key);
    }

    public String getHost() {
        return host;
    }

    public ItemKey getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostAndKey that = (HostAndKey) o;
        return Objects.equals(host, that.host) && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, key);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HostAndKey.class.getSimpleName() + "[", "]")
                .add("host='" + host + "'")
                .add("key=" + key)
                .toString();
    }
}
