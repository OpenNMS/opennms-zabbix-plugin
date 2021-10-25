package org.opennms.plugins.zabbix.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Trigger {
    private String expression;
    private String name;
    private String priority;
    private String description;
    @JsonProperty("manual_close")
    private String manualClose;

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManualClose() {
        return manualClose;
    }

    public void setManualClose(String manualClose) {
        this.manualClose = manualClose;
    }
}
