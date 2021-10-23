package org.opennms.plugins.zabbix.expressions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.MoreObjects;

public class FunctionCall implements Term {

    private final String name;
    private final List<Term> parameters;

    public FunctionCall(String name, List<Term> parameters) {
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
        FunctionCall that = (FunctionCall) o;
        return Objects.equals(name, that.name) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameters);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("parameters", parameters)
                .toString();
    }
}
