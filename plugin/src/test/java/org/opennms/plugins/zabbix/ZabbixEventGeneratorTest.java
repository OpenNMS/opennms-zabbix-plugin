package org.opennms.plugins.zabbix;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.opennms.integration.api.v1.config.events.AlarmData;
import org.opennms.integration.api.v1.config.events.AlarmType;
import org.opennms.integration.api.v1.config.events.EventDefinition;
import org.opennms.integration.api.v1.config.events.LogMessage;
import org.opennms.integration.api.v1.config.events.LogMsgDestType;
import org.opennms.integration.api.v1.model.Severity;
import org.opennms.plugins.zabbix.model.Trigger;

public class ZabbixEventGeneratorTest {

    @Test
    public void canGenerateThresholdEvents() {
        ZabbixEventGenerator zabbixEventGenerator = new ZabbixEventGenerator();

        Trigger trigger = new Trigger();
        trigger.setName("CPU queue length is too high (over {$CPU.QUEUE.CRIT.MAX} for 5m)");
        trigger.setPriority("WARNING");
        trigger.setDescription("The CPU Queue Length in the last 5 minutes exceeds {$CPU.QUEUE.CRIT.MAX}. According to actual observations, " +
                "PQL should not exceed the number of cores * 2. To fine-tune the conditions, use the macro {$CPU.QUEUE.CRIT.MAX }.");

        assertThat(ZabbixEventGenerator.getTriggerKey(trigger), equalTo("CPUqueuelengthistoohigh"));

        // threshold trigger
        EventDefinition triggerEvent = zabbixEventGenerator.getTriggerEventDefinition(trigger);
        assertThat(triggerEvent.getUei(), equalTo("uei.opennms.org/zabbixPlugin/thresholds/CPUqueuelengthistoohigh/trigger"));
        assertThat(triggerEvent.getLabel(), equalTo("Zabbix Threshold Trigger: CPUqueuelengthistoohigh"));
        assertThat(triggerEvent.getDescription(), equalTo("CPU queue length is too high (over {$CPU.QUEUE.CRIT.MAX} for 5m)"));

        LogMessage logMessage = triggerEvent.getLogMessage();
        assertThat(logMessage.getDestination(), equalTo(LogMsgDestType.LOGNDISPLAY));
        assertThat(logMessage.getContent(), equalTo("The CPU Queue Length in the last 5 minutes exceeds {$CPU.QUEUE.CRIT.MAX}. According to actual observations, " +
                "PQL should not exceed the number of cores * 2. To fine-tune the conditions, use the macro {$CPU.QUEUE.CRIT.MAX }."));
        assertThat(triggerEvent.getSeverity(), equalTo(Severity.MAJOR));

        AlarmData alarmData = triggerEvent.getAlarmData();
        assertThat(alarmData.getReductionKey(), equalTo("%uei%:%nodeid%"));
        assertThat(alarmData.getType(), equalTo(AlarmType.PROBLEM));

        // threshold re-arm
        EventDefinition rearmEvent = zabbixEventGenerator.getRearmEventDefinition(trigger);
        assertThat(rearmEvent.getUei(), equalTo("uei.opennms.org/zabbixPlugin/thresholds/CPUqueuelengthistoohigh/rearm"));
        assertThat(rearmEvent.getLabel(), equalTo("Zabbix Threshold Rearm: CPUqueuelengthistoohigh"));
        assertThat(rearmEvent.getDescription(), equalTo("RESOLVED: CPU queue length is too high (over {$CPU.QUEUE.CRIT.MAX} for 5m)"));

        logMessage = rearmEvent.getLogMessage();
        assertThat(logMessage.getDestination(), equalTo(LogMsgDestType.LOGNDISPLAY));
        assertThat(logMessage.getContent(), equalTo("RESOLVED: The CPU Queue Length in the last 5 minutes exceeds {$CPU.QUEUE.CRIT.MAX}. According to actual observations, " +
                "PQL should not exceed the number of cores * 2. To fine-tune the conditions, use the macro {$CPU.QUEUE.CRIT.MAX }."));
        assertThat(rearmEvent.getSeverity(), equalTo(Severity.CLEARED));

        alarmData = rearmEvent.getAlarmData();
        assertThat(alarmData.getReductionKey(), equalTo("%uei%:%nodeid%"));
        assertThat(alarmData.getClearKey(), equalTo("uei.opennms.org/zabbixPlugin/thresholds/CPUqueuelengthistoohigh/trigger:%nodeid%"));
        assertThat(alarmData.getType(), equalTo(AlarmType.RESOLUTION));
    }
}
