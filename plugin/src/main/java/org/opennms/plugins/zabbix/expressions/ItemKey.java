package org.opennms.plugins.zabbix.expressions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ItemKey {
    private final String name;
    private final List<String> parameters;

    public ItemKey(String name, List<String> parameters) {
        this.name = Objects.requireNonNull(name);
        this.parameters = Collections.unmodifiableList(parameters);
    }

    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        return parameters;
    }
}
