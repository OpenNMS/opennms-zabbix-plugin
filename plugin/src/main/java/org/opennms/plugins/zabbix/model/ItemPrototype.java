package org.opennms.plugins.zabbix.model;

import java.util.LinkedList;
import java.util.List;

public class ItemPrototype {
    private String name;
    private String key;
    private List<Tag> tags = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

}
