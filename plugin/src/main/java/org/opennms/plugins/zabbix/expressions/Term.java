package org.opennms.plugins.zabbix.expressions;

public interface Term {

    void visit(TermVisitor visitor);

}
