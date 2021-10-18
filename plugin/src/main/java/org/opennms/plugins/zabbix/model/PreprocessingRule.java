package org.opennms.plugins.zabbix.model;

import java.util.LinkedList;
import java.util.List;

public class PreprocessingRule {
    public static final String CHANGE_PER_SECOND = "CHANGE_PER_SECOND";

    private String type;
    private List<String> parameters = new LinkedList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }
}
