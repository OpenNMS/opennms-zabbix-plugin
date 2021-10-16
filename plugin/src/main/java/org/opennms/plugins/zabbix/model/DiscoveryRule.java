package org.opennms.plugins.zabbix.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiscoveryRule {
    private String name;
    private String key;
    private String description;
    @JsonProperty("item_prototypes")
    private List<ItemPrototype> itemPrototypes = new LinkedList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ItemPrototype> getItemPrototypes() {
        return itemPrototypes;
    }

    public void setItemPrototypes(List<ItemPrototype> itemPrototypes) {
        this.itemPrototypes = itemPrototypes;
    }
}
