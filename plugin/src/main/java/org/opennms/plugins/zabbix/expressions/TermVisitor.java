package org.opennms.plugins.zabbix.expressions;

public interface TermVisitor {
    void visitArray(Array array);

    void visitConstant(Constant constant);

    void visitExpression(Expression expression);

    void visitFunctionCall(FunctionCall functionCall);

    void visitHostAndKey(HostAndKey hostAndKey);

    void visitItemKey(ItemKey itemKey);

    void visitOperator(Operator operator);
}
