package org.opennms.plugins.zabbix.expressions;

import java.util.Objects;
import java.util.StringJoiner;

public class Operator implements Term {
    private final String value;

    public Operator(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operator operator = (Operator) o;
        return Objects.equals(value, operator.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Operator.class.getSimpleName() + "[", "]")
                .add("value='" + value + "'")
                .toString();
    }

    @Override
    public void visit(TermVisitor visitor) {
        visitor.visitOperator(this);
    }
}
