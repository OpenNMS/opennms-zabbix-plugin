package org.opennms.plugins.zabbix;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.rules.ExternalResource;
import org.opennms.plugins.zabbix.agent.ZabbixAgent;

public class MockZabbixAgent extends ExternalResource {

    private ZabbixAgent zabbixAgent;

    @Override
    protected void before() throws InterruptedException, UnknownHostException {
        zabbixAgent = new ZabbixAgent();
        zabbixAgent.start();
    }

    @Override
    protected void after() {
        if (zabbixAgent != null) {
            zabbixAgent.stop();
            zabbixAgent = null;
        }
    }

    public InetAddress getAddress() {
        return zabbixAgent.getAddress();
    }

    public int getPort() {
        return zabbixAgent.getPort();
    }
}
