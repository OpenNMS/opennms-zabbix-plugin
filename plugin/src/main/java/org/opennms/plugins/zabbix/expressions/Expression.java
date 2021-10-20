package org.opennms.plugins.zabbix.expressions;

import java.util.Collections;
import java.util.List;

public class Expression extends Parameter {

    private final String name;
    private final List<Parameter> parameters;
    private final String operator;
    private final String constant;

    public Expression(String name, List<Parameter> parameters, String operator, String constant) {
        super(name);
        this.name = name;
        this.parameters = Collections.unmodifiableList(parameters);
        this.operator = operator;
        this.constant = constant;
    }

    public String getName() {
        return name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public String getOperator() {
        return operator;
    }

    public String getConstant() {
        return constant;
    }
}
