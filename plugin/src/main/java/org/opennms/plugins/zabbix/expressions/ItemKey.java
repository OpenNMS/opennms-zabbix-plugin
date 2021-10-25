package org.opennms.plugins.zabbix.expressions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ItemKey implements Term {
    private final String name;
    private final List<Term> parameters;

    public ItemKey(String name, String... parameters) {
        this.name = Objects.requireNonNull(name);
        this.parameters = Arrays.stream(parameters)
                .map(Constant::new)
                .collect(Collectors.toList());
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemKey itemKey = (ItemKey) o;
        return Objects.equals(name, itemKey.name) && Objects.equals(parameters, itemKey.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameters);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ItemKey.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("parameters=" + parameters)
                .toString();
    }
}
