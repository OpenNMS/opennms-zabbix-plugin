package org.opennms.plugins.zabbix.mock;

import com.google.common.base.MoreObjects;

public class AgentRequest {
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key", key)
                .toString();
    }
}
