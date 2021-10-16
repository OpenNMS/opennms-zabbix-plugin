package org.opennms.plugins.zabbix;

public class ZabbixNotSupportedException extends RuntimeException {

    public ZabbixNotSupportedException(String message) {
        super(message);
    }
}
