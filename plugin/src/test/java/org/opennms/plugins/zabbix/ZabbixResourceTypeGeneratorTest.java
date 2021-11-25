package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.opennms.integration.api.v1.config.datacollection.ResourceType;
import org.opennms.integration.api.v1.config.datacollection.StrategyDefinition;
import org.opennms.integration.api.xml.schema.datacollection.Parameter;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Template;
import org.opennms.plugins.zabbix.model.TemplateMeta;

public class ZabbixResourceTypeGeneratorTest {

    private final ZabbixResourceTypeGenerator zabbixResourceTypeGenerator = new ZabbixResourceTypeGenerator();

    @Test
    public void canGenerateResourceTypesForAllKnownRules() {
        getAllDiscoveryRules()
                .forEach(rule -> {
                    System.out.println("Rule: " + rule.getName());
                    zabbixResourceTypeGenerator.getResourceTypeFor(rule);
                });
    }

    @Test
    public void canGenerateResourceTypeForFilesystemDiscovery() {
        // Find a known rule
        final DiscoveryRule fsDiscoveryRule = getDiscoveryRuleWithName("Mounted filesystem discovery");
        assertThat(fsDiscoveryRule, notNullValue());
        assertThat(fsDiscoveryRule.getTemplate(), notNullValue());

        // Generate the resource type for the rule
        final ResourceType resourceType = zabbixResourceTypeGenerator.getResourceTypeFor(fsDiscoveryRule);

        // Verify
        // rule key -> resource type name (i.e. id)
        assertThat(resourceType.getName(), equalTo("vfsfsdiscovery"));
        // rule name -> resource type label
        assertThat(resourceType.getLabel(), equalTo(fsDiscoveryRule.getName()));
        // first tag value -> resource label
        assertThat(resourceType.getResourceLabel(), equalTo("Filesystem ${FSNAME}"));
        // storage
        StrategyDefinition storageStrategy = resourceType.getStorageStrategy();
        assertThat(storageStrategy.getClazz(), equalTo(ZabbixResourceTypeGenerator.SIBLING_STRAT_CLASS));
        assertThat(storageStrategy.getParameters(), hasItem(new Parameter("sibling-column-name", "FSNAME")));
        // persistence
        StrategyDefinition persistenceStrategy = resourceType.getPersistenceSelectorStrategy();
        assertThat(persistenceStrategy.getClazz(), equalTo(ZabbixResourceTypeGenerator.REGEX_STRAT_CLASS));
        assertThat(persistenceStrategy.getParameters(), hasItem(new Parameter("match-expression", "(#FSNAME not matches '^(/dev|/sys|/run|/proc|.+/shm$)')")));
    }

    @Test
    public void canGenerateResourceTypeForPhysicalDiskDiscovery() {
        // Find a known rule
        final DiscoveryRule diskDiscoveryRule = getDiscoveryRuleWithName("Physical disks discovery");
        assertThat(diskDiscoveryRule, notNullValue());
        assertThat(diskDiscoveryRule.getTemplate(), notNullValue());

        // Generate the resource type for the rule
        final ResourceType resourceType = zabbixResourceTypeGenerator.getResourceTypeFor(diskDiscoveryRule);

        // Verify
        // rule key -> resource type name (i.e. id)
        assertThat(resourceType.getName(), equalTo("perfinstanceendiscoveryPhysicalDisk"));
        // rule name -> resource type label
        assertThat(resourceType.getLabel(), equalTo(diskDiscoveryRule.getName()));
        // first tag value -> resource label
        assertThat(resourceType.getResourceLabel(), equalTo("Disk ${DEVNAME}"));
        // storage
        StrategyDefinition storageStrategy = resourceType.getStorageStrategy();
        assertThat(storageStrategy.getClazz(), equalTo(ZabbixResourceTypeGenerator.SIBLING_STRAT_CLASS));
        assertThat(storageStrategy.getParameters(), hasItem(new Parameter("sibling-column-name", "DEVNAME")));
        // persistence
        StrategyDefinition persistenceStrategy = resourceType.getPersistenceSelectorStrategy();
        assertThat(persistenceStrategy.getClazz(), equalTo(ZabbixResourceTypeGenerator.REGEX_STRAT_CLASS));
        assertThat(persistenceStrategy.getParameters(), hasItem(new Parameter("match-expression", "(#DEVNAME not matches '_Total')")));
    }

    private List<DiscoveryRule> getAllDiscoveryRules() {
        List<DiscoveryRule> discoveryRules = new LinkedList<>();
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        for (TemplateMeta templateMeta : zabbixTemplateHandler.loadTemplates()) {
            for (Template template : templateMeta.getZabbixExport().getTemplates()) {
                discoveryRules.addAll(template.getDiscoveryRules());
            }
        }
        return discoveryRules;
    }

    private DiscoveryRule getDiscoveryRuleWithName(String name) {
        return getAllDiscoveryRules().stream()
                .filter(rule -> name.equals(rule.getName()))
                .findFirst().orElse(null);
    }
}
