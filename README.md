# OpenNMS Zabbix Plugin

## Building

Build and install the plugin into your local Maven repository using:
```
mvn clean install
```

> OpenNMS normally runs as root, so make sure the artifacts are installed in `/root/.m2` or try making `/root/.m2` symlink to your user's repository

From the OpenNMS Karaf shell:
```
feature:repo-add mvn:org.opennms.plugins.zabbix/karaf-features/1.0.0-SNAPSHOT/xml
feature:install opennms-plugins-zabbix
```

Update automatically:
```
bundle:watch *
```

## Using

Run a collection attemp:
```
collect org.opennms.plugins.zabbix.ZabbixAgentCollector 127.0.0.1
```
