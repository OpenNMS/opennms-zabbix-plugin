package org.opennms.plugins.zabbix.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Template {

    private List<Item> items = new LinkedList<>();

    @JsonProperty("discovery_rules")
    private List<DiscoveryRule> discoveryRules = new LinkedList<>();

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<DiscoveryRule> getDiscoveryRules() {
        return discoveryRules;
    }

    public void setDiscoveryRules(List<DiscoveryRule> discoveryRules) {
        this.discoveryRules = discoveryRules;
    }
}
