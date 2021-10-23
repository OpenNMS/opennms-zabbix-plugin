package org.opennms.plugins.zabbix.expressions;

import java.util.LinkedList;
import java.util.List;

public class ItemKey {
    private String name;
    private List<String> parameters = new LinkedList<>();

    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        return parameters;
    }
}
