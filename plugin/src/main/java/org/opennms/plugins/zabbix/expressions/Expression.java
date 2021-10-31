package org.opennms.plugins.zabbix.expressions;

import java.util.Objects;
import java.util.StringJoiner;

public class Expression implements Term {

    private final Term lhs, rhs;
    private final Operator operator;

    public Expression(Term lhs) {
        this.lhs = Objects.requireNonNull(lhs);
        this.operator = null;
        this.rhs = null;
    }

    public Expression(Term lhs, Operator operator, Term rhs) {
        this.lhs = Objects.requireNonNull(lhs);
        this.operator = operator;
        this.rhs = rhs;
    }

    public Term getLhs() {
        return lhs;
    }

    public Term getRhs() {
        return rhs;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expression that = (Expression) o;
        return Objects.equals(lhs, that.lhs) && Objects.equals(rhs, that.rhs) && Objects.equals(operator, that.operator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs, operator);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Expression.class.getSimpleName() + "[", "]")
                .add("lhs=" + lhs)
                .add("rhs=" + rhs)
                .add("operator='" + operator + "'")
                .toString();
    }

    @Override
    public void visit(TermVisitor visitor) {
        visitor.visitExpression(this);
    }
}
