package org.opennms.plugins.zabbix.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Condition {
    private String macro;
    private String value;
    private String operator;
    @JsonProperty("formulaid")
    private String formulaId;

    public String getMacro() {
        return macro;
    }

    public void setMacro(String macro) {
        this.macro = macro;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getFormulaId() {
        return formulaId;
    }

    public void setFormulaId(String formulaId) {
        this.formulaId = formulaId;
    }
}
