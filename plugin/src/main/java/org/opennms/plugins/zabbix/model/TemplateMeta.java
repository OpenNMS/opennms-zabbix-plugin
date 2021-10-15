package org.opennms.plugins.zabbix.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TemplateMeta {

    @JsonProperty("zabbix_export")
    private ZabbixExport zabbixExport;

    public ZabbixExport getZabbixExport() {
        return zabbixExport;
    }

    public void setZabbixExport(ZabbixExport zabbixExport) {
        this.zabbixExport = zabbixExport;
    }

}
