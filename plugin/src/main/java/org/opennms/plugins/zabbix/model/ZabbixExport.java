package org.opennms.plugins.zabbix.model;

import java.util.LinkedList;
import java.util.List;

public class ZabbixExport {
    private List<Template> templates = new LinkedList<>();

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }
}
