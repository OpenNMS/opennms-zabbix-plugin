package org.opennms.plugins.zabbix;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.opennms.integration.api.v1.config.events.AlarmData;
import org.opennms.integration.api.v1.config.events.AlarmType;
import org.opennms.integration.api.v1.config.events.EventDefinition;
import org.opennms.integration.api.v1.config.events.LogMessage;
import org.opennms.integration.api.v1.config.events.LogMsgDestType;
import org.opennms.integration.api.v1.config.events.ManagedObject;
import org.opennms.integration.api.v1.config.events.Mask;
import org.opennms.integration.api.v1.config.events.Parameter;
import org.opennms.integration.api.v1.config.events.UpdateField;
import org.opennms.integration.api.v1.model.Severity;
import org.opennms.plugins.zabbix.model.Trigger;

public class ZabbixEventGenerator {

    public List<EventDefinition> getEventDefinitions(Trigger trigger) {
        return Arrays.asList(getTriggerEventDefinition(trigger), getRearmEventDefinition(trigger));
    }

    public EventDefinition getTriggerEventDefinition(Trigger trigger) {
        return new ZabbixTriggerEventDefinition(trigger);
    }

    public EventDefinition getRearmEventDefinition(Trigger trigger) {
        return new ZabbixRearmEventDefinition(trigger);
    }

    public static String getTriggerKey(Trigger trigger) {
        // filter unsupported characters
        final StringBuilder sb = new StringBuilder();
        for (int i=0; i < trigger.getName().length(); i++) {
            char b = trigger.getName().charAt(i);
            if (!((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9' && i > 0))) {
                if (b == '(') {
                    break;
                }
            } else {
                sb.append(b);
            }
            if (i > 32) {
                break;
            }
        }
        return sb.toString();
    }

    public static class ZabbixTriggerEventDefinition extends ZabbixEventDefinition {
        private final Trigger trigger;
        private final String triggerKey;

        public ZabbixTriggerEventDefinition(Trigger trigger) {
            this.trigger = Objects.requireNonNull(trigger);
            triggerKey = getTriggerKey(trigger);
        }

        @Override
        public String getUei() {
            return String.format("uei.opennms.org/zabbixPlugin/thresholds/%s/trigger", triggerKey);
        }

        @Override
        public String getLabel() {
            return "Zabbix Threshold Trigger: " + triggerKey;
        }

        @Override
        public Severity getSeverity() {
            return Severity.MAJOR;
        }

        @Override
        public String getDescription() {
            return trigger.getName();
        }

        @Override
        public LogMessage getLogMessage() {
            return new LogMessage() {

                @Override
                public String getContent() {
                    return trigger.getDescription();
                }

                @Override
                public LogMsgDestType getDestination() {
                    return LogMsgDestType.LOGNDISPLAY;
                }
            };
        }

        @Override
        public AlarmData getAlarmData() {
            return new AlarmData() {
                @Override
                public String getReductionKey() {
                    return "%uei%:%nodeid%";
                }

                @Override
                public AlarmType getType() {
                    return AlarmType.PROBLEM;
                }

                @Override
                public String getClearKey() {
                    return null;
                }

                @Override
                public boolean isAutoClean() {
                    return false;
                }

                @Override
                public List<UpdateField> getUpdateFields() {
                    return Collections.emptyList();
                }

                @Override
                public ManagedObject getManagedObject() {
                    return null;
                }
            };
        }
    }

    public static class ZabbixRearmEventDefinition extends ZabbixEventDefinition {
        private final Trigger trigger;
        private final String triggerKey;

        public ZabbixRearmEventDefinition(Trigger trigger) {
            this.trigger = Objects.requireNonNull(trigger);
            triggerKey = getTriggerKey(trigger);
        }

        @Override
        public String getUei() {
            return String.format("uei.opennms.org/zabbixPlugin/thresholds/%s/rearm", triggerKey);
        }

        @Override
        public String getLabel() {
            return "Zabbix Threshold Rearm: " + triggerKey;
        }

        @Override
        public Severity getSeverity() {
            return Severity.CLEARED;
        }

        @Override
        public String getDescription() {
            return "RESOLVED: " + trigger.getName();
        }

        @Override
        public LogMessage getLogMessage() {
            return new LogMessage() {

                @Override
                public String getContent() {
                    return "RESOLVED: " +trigger.getDescription();
                }

                @Override
                public LogMsgDestType getDestination() {
                    return LogMsgDestType.LOGNDISPLAY;
                }
            };
        }

        @Override
        public AlarmData getAlarmData() {
            return new AlarmData() {
                @Override
                public String getReductionKey() {
                    return "%uei%:%nodeid%";
                }

                @Override
                public AlarmType getType() {
                    return AlarmType.RESOLUTION;
                }

                @Override
                public String getClearKey() {
                    return String.format("uei.opennms.org/zabbixPlugin/thresholds/%s/trigger:%%nodeid%%", triggerKey);
                }

                @Override
                public boolean isAutoClean() {
                    return false;
                }

                @Override
                public List<UpdateField> getUpdateFields() {
                    return Collections.emptyList();
                }

                @Override
                public ManagedObject getManagedObject() {
                    return null;
                }
            };
        }
    }

    public static abstract class ZabbixEventDefinition implements EventDefinition {
        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public String getOperatorInstructions() {
            return null;
        }

        @Override
        public Mask getMask() {
            return null;
        }

        @Override
        public List<Parameter> getParameters() {
            return Collections.emptyList();
        }
    }
}
