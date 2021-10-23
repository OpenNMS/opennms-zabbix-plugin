package org.opennms.plugins.zabbix.expressions;

public class HostAndKey {
    private String host;
    private ItemKey key;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public ItemKey getKey() {
        return key;
    }

    public void setKey(ItemKey key) {
        this.key = key;
    }
}
