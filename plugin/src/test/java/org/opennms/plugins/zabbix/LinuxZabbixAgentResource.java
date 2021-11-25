package org.opennms.plugins.zabbix;

import java.net.UnknownHostException;

import org.opennms.plugins.zabbix.agent.ZabbixAgent;

public class LinuxZabbixAgentResource extends AbstractAgentResource {
    @Override
    protected void before() throws InterruptedException, UnknownHostException {
        zabbixAgent = new ZabbixAgent();
        zabbixAgent.start();
    }
}
