# OpenNMS Zabbix Plugin

This plugin is an effort to have OpenNMS integrate with the Zabbix Agent and leverage the existing knowledge base of Zabbix templates available.

## Mapping from keys to resources

Item keys in Zabbix look like:
* `vm.memory.utilization`
* `vfs.file.contents[/sys/block/{#DEVNAME}/stat]`
* `system.cpu.util[,iowait]`

If no macro is present in the key, we assume that there is only a single metric per agent (or node).
We place these on the "node level resource".

`vm.memory.utilization` -> `node[1].nodeResource[].vm.memory.utilization`

If a single macro is present in the key, we assume that this macro represents a unique index.
This indexed is used to build a "generic resource" with the name of the discovery rule as the resource type name.

`vfs.file.contents[/sys/block/{#DEVNAME}/stat]` -> `node[1].vfs.dev.discovery[/sys/block/md0/stat].nvfs.file.contents`

Multiple macros are not currently supported.

## Macros

Zabbix support many forms of macros that look like:
* General: `{MACRO}`
* Low level discovery macro: `{#MACRO}`
* User macro: `{$MACRO}`
* User macro with context: `{$MACRO:"static text"}`
* User macro with regular expressions: `{$MACRO:regex:"regular expression"}`
* Macro with functions: `{{#IFALIAS}.regsub("(.*)_([0-9]+)", \1)}`

Support for macros is currently quite limited.

## Building

Build and install the plugin into your local Maven repository using:
```
mvn clean install
```

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

1. Load the plugin by following the build instructions above.

2. Provision a node with an IP address pointed to the agent under test.

3. Configure which template to use for collection by setting the value of the 'zabbix.template' key in the 'requisition' context for meta-data:

> For example: zabbix.template=Linux by Zabbix agent

4. Run a collection attempt

```
collect -n 4 -p  org.opennms.plugins.zabbix.ZabbixAgentCollector 127.0.0.1
```

```
...
NodeLevelResource[nodeId=4, path=null]
        Group: zabbix
                Attribute[system.cpu.load.all.avg1:2.374023]
                Attribute[system.cpu.load.all.avg5:2.859863]
                Attribute[system.cpu.load.all.avg15:2.970215]
                Attribute[system.cpu.num:16.0]
                Attribute[kernel.maxfiles:196608.0]
                Attribute[kernel.maxproc:16704.0]
                Attribute[system.boottime:1.63404391E9]
                Attribute[system.localtime:1.634520601E9]
                Attribute[system.uptime:476691.0]
                Attribute[system.users.num:2.0]
                Attribute[vm.memory.size.available:3.136821248E10]
                Attribute[vm.memory.size.pavailable:45.646757]
                Attribute[vm.memory.size.total:6.8719476736E10]
                Attribute[agent.ping:1.0]
                Attribute[system.hostname:whyousosnoopy]
                Attribute[system.sw.arch:x86_64]
                Attribute[system.uname:Darwin whyousosnoopy 20.6.0 Darwin Kernel Version 20.6.0]
                Attribute[agent.hostname:Zabbix server]
                Attribute[agent.version:5.4.4]
```

## Installing

OpenNMS:

```
echo 'opennms-plugins-zabbix wait-for-kar=opennms-zabbix-plugin' $OPENNMS_HOME/etc/featuresBoot.d/zabbix.boot
```

