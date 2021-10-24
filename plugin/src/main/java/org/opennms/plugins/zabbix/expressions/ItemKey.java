package org.opennms.plugins.zabbix.expressions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ItemKey {
    private final String name;
    private final List<Term> parameters;

    public ItemKey(String name, List<Term> parameters) {
        this.name = Objects.requireNonNull(name);
        this.parameters = Collections.unmodifiableList(parameters);
    }

    public String getName() {
        return name;
    }

    public List<Term> getParameters() {
        return parameters;
    }
}
