package org.opennms.plugins.zabbix.model;

import java.util.LinkedList;
import java.util.List;

public class Template {

    private List<Item> items = new LinkedList<>();

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
