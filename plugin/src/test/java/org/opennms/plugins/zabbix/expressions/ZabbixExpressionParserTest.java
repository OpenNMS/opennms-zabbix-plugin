package org.opennms.plugins.zabbix.expressions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

public class ZabbixExpressionParserTest {
    private ExpressionParser parser = new ExpressionParser();

    @Test
    public void canParseExpression() throws org.opennms.plugins.zabbix.expressions.ParseException {
        Expression expression = parser.parse("max(123,1) + 5");
        assertThat(expression.getName(), equalTo("max"));
        assertThat(expression.getParameters(), contains(new Parameter("123"), new Parameter("1")));
        assertThat(expression.getOperator(), equalTo("+"));
        assertThat(expression.getConstant(), equalTo("5"));

        expression = parser.parse("min(/Linux block devices by Zabbix agent/vfs.dev.read.await[{#DEVNAME}],15m) > {$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"}");
        assertThat(expression.getName(), equalTo("min"));
        System.out.println(expression.getParameters());
        assertThat(expression.getParameters(), contains(
                new Parameter("/Linux block devices by Zabbix agent/vfs.dev.read.await[{#DEVNAME}]"),
                new Parameter("15m")));
        assertThat(expression.getOperator(), equalTo(">"));
        assertThat(expression.getConstant(), equalTo("{$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"}"));
    }

}
