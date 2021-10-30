package org.opennms.plugins.zabbix.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Template {

    private String name;
    private String description;

    @JsonManagedReference
    private List<Item> items = new LinkedList<>();

    @JsonManagedReference
    @JsonProperty("discovery_rules")
    private List<DiscoveryRule> discoveryRules = new LinkedList<>();

    private List<Macro> macros = new LinkedList<>();

    @JsonProperty("templates")
    private List<LinkedTemplate> linkedTemplates = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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

    public List<Macro> getMacros() {
        return macros;
    }

    public void setMacros(List<Macro> macros) {
        this.macros = macros;
    }

    public List<LinkedTemplate> getLinkedTemplates() {
        return linkedTemplates;
    }

    public void setLinkedTemplates(List<LinkedTemplate> linkedTemplates) {
        this.linkedTemplates = linkedTemplates;
    }

    @JsonIgnore
    public Optional<DiscoveryRule> getDiscoveryRuleByName(String name) {
        return discoveryRules.stream()
                .filter(rule -> Objects.equals(name, rule.getName()))
                .findFirst();
    }
}
