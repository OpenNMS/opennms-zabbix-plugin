package org.opennms.plugins.zabbix.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Graph {
    private String name;
    @JsonProperty("graph_items")
    private List<GraphItem> graphItems = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GraphItem> getGraphItems() {
        return graphItems;
    }

    public void setGraphItems(List<GraphItem> graphItems) {
        this.graphItems = graphItems;
    }
}
