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
        FunctionCall fn = (FunctionCall)expression.getLhs();
        assertThat(fn.getName(), equalTo("max"));
        assertThat(fn.getParameters(), contains(new Constant("123"), new Constant("1")));
        assertThat(expression.getOperator(), equalTo("+"));
        Constant constant = (Constant)expression.getRhs();
        assertThat(constant.getValue(), equalTo("5"));

        expression = parser.parse("min(/Linux block devices by Zabbix agent/vfs.dev.read.await[{#DEVNAME}],15m) > {$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"}");
        fn = (FunctionCall)expression.getLhs();
        assertThat(fn.getName(), equalTo("min"));
        assertThat(fn.getParameters(), contains(
                new Constant("/Linux block devices by Zabbix agent/vfs.dev.read.await[{#DEVNAME}]"),
                new Constant("15m")));
        assertThat(expression.getOperator(), equalTo(">"));
        constant = (Constant)expression.getRhs();
        assertThat(constant.getValue(), equalTo("{$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"}"));

        expression = parser.parse("min(/Linux block devices by Zabbix agent/vfs.dev.read.await[{#DEVNAME}],15m) > {$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"}");
        fn = (FunctionCall)expression.getLhs();
        assertThat(fn.getName(), equalTo("min"));
        assertThat(fn.getParameters(), contains(
                new Constant("/Linux block devices by Zabbix agent/vfs.dev.read.await[{#DEVNAME}]"),
                new Constant("15m")));
        assertThat(expression.getOperator(), equalTo(">"));
        constant = (Constant)expression.getRhs();
        assertThat(constant.getValue(), equalTo("{$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"}"));

        expression = parser.parse("{$IFCONTROL:\"{#IFNAME}\"}=0");
        constant = (Constant)expression.getLhs();
        assertThat(constant.getValue(), equalTo("{$IFCONTROL:\"{#IFNAME}\"}"));
        constant = (Constant)expression.getRhs();
        assertThat(constant.getValue(), equalTo("0"));

        expression = parser.parse("min(/Linux block devices by Zabbix agent/vfs.dev.read.await[{#DEVNAME}],15m) > {$VFS.DEV.READ.AWAIT.WARN:\"{#DEVNAME}\"} or min(/Linux block devices by Zabbix agent/vfs.dev.write.await[{#DEVNAME}],15m) > {$VFS.DEV.WRITE.AWAIT.WARN:\"{#DEVNAME}\"}");
        Expression lhsExression = (Expression)expression.getLhs();
        fn = (FunctionCall)lhsExression.getLhs();
        assertThat(fn.getName(), equalTo("min"));
        assertThat(expression.getOperator(), equalTo("or"));
        Expression rhsExpression = (Expression)expression.getRhs();
        fn = (FunctionCall)rhsExpression.getLhs();
        assertThat(fn.getName(), equalTo("min"));


        expression = parser.parse("max(1,2)>5 or max(2,3)>4");
        expression = parser.parse("max(1,2)>5 or (max(2,3)>4 and max(1,2)<4)");
        //expression = parser.parse("timeleft(/host/vfs.fs.size[/,free],1h,0)");
       // expression = parser.parse("timeleft(/host/vfs.fs.size[a,free],1h,0)<1h and ({TRIGGER.VALUE}=0 and timeleft(/host/vfs.fs.size[a,free],1h,0)<>-1 or {TRIGGER.VALUE}=1)");
    }

}
