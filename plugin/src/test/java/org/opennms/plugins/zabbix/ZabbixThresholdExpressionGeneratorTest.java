package org.opennms.plugins.zabbix;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.opennms.integration.api.v1.config.thresholding.Expression;
import org.opennms.integration.api.v1.config.thresholding.ThresholdType;
import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Item;
import org.opennms.plugins.zabbix.model.Macro;
import org.opennms.plugins.zabbix.model.Template;
import org.opennms.plugins.zabbix.model.Trigger;

public class ZabbixThresholdExpressionGeneratorTest {
    private final ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
    private final ZabbixThresholdExpressionGenerator zabbixThresholdExpressionGenerator = new ZabbixThresholdExpressionGenerator(zabbixTemplateHandler);


    @Test
    public void canGenerateExpression() {
        // Simple
        Trigger trigger = new Trigger();
        trigger.setName("CPU interrupt time is too high (over {$CPU.INTERRUPT.CRIT.MAX}% for 5m)");
        trigger.setExpression("min(/Windows CPU by Zabbix agent/perf_counter_en[\"\\Processor Information(_total)\\% Interrupt Time\"],5m)>{$CPU.INTERRUPT.CRIT.MAX}");

        Item item = new Item();
        trigger.setItem(item);

        Template template = new Template();
        item.setTemplate(template);

        Macro macro = new Macro();
        macro.setMacro("{$CPU.INTERRUPT.CRIT.MAX}");
        macro.setValue("50");
        template.setMacros(Collections.singletonList(macro));

        Expression threshExpression = zabbixThresholdExpressionGenerator.parse(trigger);
        assertThat(threshExpression.getExpression(), equalTo("(datasources['perf_counter_en.processor.information._total.interrupt.time'] > 50.00) ? 1 : 0"));
        assertThat(threshExpression.getDsType(), equalTo("node"));
        assertThat(threshExpression.getTrigger(), equalTo(1));
        assertThat(threshExpression.getRearm(), equalTo(0.0d));
        assertThat(threshExpression.getValue(), equalTo(1.0d));
        assertThat(threshExpression.getType(), equalTo(ThresholdType.HIGH));

        // Compound
        trigger = new Trigger();
        trigger.setExpression("min(/Linux block devices by Zabbix agent/vfs.dev.read.await[{#DEVNAME}],15m) > {$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"}" +
                " or min(/Linux block devices by Zabbix agent/vfs.dev.write.await[{#DEVNAME}],15m) > {$VFS.DEV.WRITE.AWAIT.WARN:\"{#DEVNAME}\"}");
        trigger.setName("{#DEVNAME}: Disk read/write request responses are too high (read > {$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"} ms for 15m or write > {$VFS.DEV.WRITE.AWAIT.WARN:\"{#DEVNAME}\"} ms for 15m)");
        trigger.setDescription("This trigger might indicate disk {#DEVNAME} saturation.");
        trigger.setItem(item);

        Macro readAwait = new Macro();
        readAwait.setMacro("{$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"}");
        readAwait.setValue("5");

        Macro writeAwait = new Macro();
        writeAwait.setMacro("{$VFS.DEV.WRITE.AWAIT.WARN:\"{#DEVNAME}\"}");
        writeAwait.setValue("6");
        template.setMacros(Arrays.asList(readAwait, writeAwait));

        DiscoveryRule discoveryRule = new DiscoveryRule();
        discoveryRule.setKey("vfs.dev.discovery");
        trigger.setDiscoveryRule(discoveryRule);

        threshExpression = zabbixThresholdExpressionGenerator.parse(trigger);
        assertThat(threshExpression.getExpression(), equalTo("((datasources['vfs.dev.read.await'] > 5.00) ? 1 : 0) " +
                "+ ((datasources['vfs.dev.write.await'] > 6.00) ? 1 : 0)"));
        assertThat(threshExpression.getDsType(), equalTo("vfs.dev.discovery"));
        assertThat(threshExpression.getTrigger(), equalTo(1));
        assertThat(threshExpression.getRearm(), equalTo(0.0d));
        assertThat(threshExpression.getValue(), equalTo(1.0d));
        assertThat(threshExpression.getType(), equalTo(ThresholdType.HIGH));
    }

    @Test
    public void canGenerateExpressions() {
        // Make sure at least one can parse to an expression
        List<ZabbixThresholdExpressionGenerator.ZabbixThresholdingExpression> expressions = zabbixTemplateHandler.getTemplates().stream()
                .flatMap(t -> t.getItems().stream())
                .flatMap(r -> r.getTriggers().stream())
                .map(zabbixThresholdExpressionGenerator::tryToParse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        assertThat(expressions, not(empty()));

        ZabbixThresholdExpressionGenerator.ZabbixThresholdingExpression expression = expressions.stream()
                        .filter(t -> "The Memory Free System Page Table Entries is less than {$MEM.PAGE_TABLE_CRIT.MIN} for 5 minutes. If the number is less than 5,000, there may well be a memory leak.".equals(t.getDescription().get()))
                                .findFirst().orElseThrow(() -> new RuntimeException("expected expression not found"));
        assertThat(expression.getType(), equalTo(ThresholdType.HIGH));
        assertThat(expression.getDsType(), equalTo("node"));
        assertThat(expression.getValue(), equalTo(1.0d));
        assertThat(expression.getRearm(), equalTo(0.0d));
        assertThat(expression.getTrigger(), equalTo(1));
        assertThat(expression.getExpression(), equalTo("(datasources['perf_counter_en.memory.free.system.page.table.entries'] < 5000.00) ? 1 : 0"));
    }
}
