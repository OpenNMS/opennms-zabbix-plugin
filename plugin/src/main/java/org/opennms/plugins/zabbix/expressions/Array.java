package org.opennms.plugins.zabbix.expressions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class Array implements Term {
    private final List<Term> parameters;

    public Array(List<Term> parameters) {
        this.parameters = Collections.unmodifiableList(parameters);
    }

    public List<Term> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Array array = (Array) o;
        return Objects.equals(parameters, array.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Array.class.getSimpleName() + "[", "]")
                .add("parameters=" + parameters)
                .toString();
    }
}
