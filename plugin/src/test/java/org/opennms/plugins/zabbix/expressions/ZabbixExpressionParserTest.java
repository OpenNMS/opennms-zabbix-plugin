package org.opennms.plugins.zabbix.expressions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.Ignore;
import org.junit.Test;
import org.opennms.plugins.zabbix.items.ItemParser;

public class ZabbixExpressionParserTest {
    private ExpressionParser parser = new ExpressionParser();

    /**
     * See https://www.zabbix.com/documentation/current/manual/config/items/item/key for syntax
     */
    @Test
    public void canParseKey() throws org.opennms.plugins.zabbix.items.ParseException {
        ItemParser itemParser = new ItemParser();

        // simple key
        ItemKey itemKey = itemParser.parse("agent.ping");
        assertThat(itemKey.getParameters(), hasSize(0));
        // no parameters
        itemKey = itemParser.parse("icmpping[]");
        assertThat(itemKey.getParameters(), hasSize(0));
        // multiple parameters
        itemKey = itemParser.parse("icmpping[,,200,,500]");
        assertThat(itemKey.getParameters(), hasSize(5));
        // single parameter containing /
        itemKey = itemParser.parse("vfs.file.contents[/sys/block/{#DEVNAME}/stat]");
        assertThat(itemKey.getParameters(), hasSize(1));
        // array handling
        itemKey = itemParser.parse("key[[1,2,3],[4,5,6],]");
        assertThat(itemKey.getParameters(), hasSize(2));
        // quote handling
        itemKey = itemParser.parse("key[\"hey this comma,is quoted\"]");
        assertThat(itemKey.getParameters(), hasSize(1));
        // quote handling
        itemKey = itemParser.parse("key[\"hey this quote\\\"is quoted\"]");
        assertThat(itemKey.getParameters(), hasSize(1));
    }

    /**
     * Alphanumerics, spaces, dots, dashes and underscores are allowed.
     */
    @Test
    @Ignore
    public void canParseHostAndKey() throws org.opennms.plugins.zabbix.expressions.ParseException {
        // simple key
        HostAndKey hostAndKey = parser.parseHostAndKey("/a/b");
        assertThat(hostAndKey.getHost(), equalTo("a"));
        assertThat(hostAndKey.getKey().getName(), equalTo("b"));

        // complex key
        hostAndKey = parser.parseHostAndKey("/a1.b2.d-9.c_7/b[1]");
        assertThat(hostAndKey.getHost(), equalTo("a1.b2.d-9.c_7"));
        assertThat(hostAndKey.getKey().getName(), equalTo("b"));

        // key with /
        hostAndKey = parser.parseHostAndKey("/host/vfs.fs.size[/,free]");
        assertThat(hostAndKey.getHost(), equalTo("host"));
        assertThat(hostAndKey.getKey().getName(), equalTo("vfs.fs.size"));
        assertThat(hostAndKey.getKey().getParameters().get(0), equalTo("/"));
    }

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
