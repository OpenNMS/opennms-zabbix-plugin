package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;
import org.opennms.integration.api.v1.config.datacollection.ResourceType;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Template;
import org.opennms.plugins.zabbix.model.TemplateMeta;

public class ZabbixResourceTypeGeneratorTest {

    @Test
    public void canGenerateResourceTypeForFilesystemDiscovery() {
        /// Find a known rule
        final DiscoveryRule fsDiscoveryRule = getDiscoveryRuleWithName("Mounted filesystem discovery");
        assertThat(fsDiscoveryRule, notNullValue());

        // Generate the resource type for the rule
        ZabbixResourceTypeGenerator zabbixResourceTypeGenerator = new ZabbixResourceTypeGenerator();
        final ResourceType resourceType = zabbixResourceTypeGenerator.getResourceTypeFor(fsDiscoveryRule);

        // Verify
        // rule key -> resource type name (i.e. id)
        assertThat(resourceType.getName(), equalTo(fsDiscoveryRule.getKey()));
        // rule name -> resource type label
        assertThat(resourceType.getLabel(), equalTo(fsDiscoveryRule.getName()));
        // first tag value -> resource label
        assertThat(resourceType.getResourceLabel(), equalTo("Filesystem ${FSNAME}"));
    }

    @Test
    public void canGenerateResourceTypeForPhysicalDiskDiscovery() {
        /// Find a known rule
        final DiscoveryRule diskDiscoveryRule = getDiscoveryRuleWithName("Physical disks discovery");
        assertThat(diskDiscoveryRule, notNullValue());

        // Generate the resource type for the rule
        ZabbixResourceTypeGenerator zabbixResourceTypeGenerator = new ZabbixResourceTypeGenerator();
        final ResourceType resourceType = zabbixResourceTypeGenerator.getResourceTypeFor(diskDiscoveryRule);

        // Verify
        // rule key -> resource type name (i.e. id)
        assertThat(resourceType.getName(), equalTo(diskDiscoveryRule.getKey()));
        // rule name -> resource type label
        assertThat(resourceType.getLabel(), equalTo(diskDiscoveryRule.getName()));
        // first tag value -> resource label
        assertThat(resourceType.getResourceLabel(), equalTo("Disk ${DEVNAME}"));
    }

    @Test
    public void canConvertMacrosToVariables() {
        // base cases, no macros
        assertThat(ZabbixResourceTypeGenerator.macrosToVariables(null), equalTo(null));
        assertThat(ZabbixResourceTypeGenerator.macrosToVariables(""), equalTo(""));
        assertThat(ZabbixResourceTypeGenerator.macrosToVariables("why?"), equalTo("why?"));
        // basic replacement
        assertThat(ZabbixResourceTypeGenerator.macrosToVariables("{#FSNAME}"), equalTo("${FSNAME}"));
        // incomplete + complete
        assertThat(ZabbixResourceTypeGenerator.macrosToVariables("{#FSNAME {#FSNAME}"), equalTo("{#FSNAME ${FSNAME}"));
        // multiple replacement
        assertThat(ZabbixResourceTypeGenerator.macrosToVariables("{#FSNAME} {#DISKNAME} {#DEVNAME}"),
                equalTo("${FSNAME} ${DISKNAME} ${DEVNAME}"));
    }

    private DiscoveryRule getDiscoveryRuleWithName(String name) {
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        for (TemplateMeta templateMeta : zabbixTemplateHandler.loadTemplates()) {
            for (Template template : templateMeta.getZabbixExport().getTemplates()) {
                for (DiscoveryRule rule : template.getDiscoveryRules()) {
                    if (name.equals(rule.getName())) {
                        return rule;
                    }
                }
            }
        }
        return null;
    }
}
