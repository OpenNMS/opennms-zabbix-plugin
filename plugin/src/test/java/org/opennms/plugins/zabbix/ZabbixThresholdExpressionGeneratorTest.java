package org.opennms.plugins.zabbix;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.opennms.integration.api.v1.config.thresholding.ThresholdType;

public class ZabbixThresholdExpressionGeneratorTest {

    @Test
    public void canGenerateExpressions() {
        ZabbixTemplateHandler zabbixTemplateHandler = new ZabbixTemplateHandler();
        ZabbixThresholdExpressionGenerator zabbixThresholdExpressionGenerator = new ZabbixThresholdExpressionGenerator(zabbixTemplateHandler);

        // Make sure at least one can parse to an expression
        List<ZabbixThresholdExpressionGenerator.ZabbixThresholdingExpression> expressions = zabbixTemplateHandler.getTemplates().stream()
                .flatMap(t -> t.getItems().stream())
                .flatMap(r -> r.getTriggers().stream())
                .map(zabbixThresholdExpressionGenerator::tryToParse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        assertThat(expressions, not(empty()));

        ZabbixThresholdExpressionGenerator.ZabbixThresholdingExpression firstExpression = expressions.get(0);
        assertThat(firstExpression.getZabbixTrigger().getDescription(), equalTo("The Memory Free System Page Table Entries is less than {$MEM.PAGE_TABLE_CRIT.MIN} for 5 minutes. If the number is less than 5,000, there may well be a memory leak."));
        assertThat(firstExpression.getType(), equalTo(ThresholdType.LOW));
        assertThat(firstExpression.getDsType(), equalTo("node"));
        assertThat(firstExpression.getValue(), equalTo(5000d)); // defaults to 5000
        assertThat(firstExpression.getRearm(), equalTo(5001d)); // value + 1
        assertThat(firstExpression.getTrigger(), equalTo(1)); // one data point is sufficient
        assertThat(firstExpression.getExpression(), equalTo("perf_counter_en.\"\\Memory\\Free System Page Table Entries\""));
    }
}
