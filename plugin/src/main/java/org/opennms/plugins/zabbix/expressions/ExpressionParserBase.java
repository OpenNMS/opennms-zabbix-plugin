package org.opennms.plugins.zabbix.expressions;

import java.io.Reader;
import java.io.StringReader;

public abstract class ExpressionParserBase {

    // Generated functions
    public abstract void ReInit(Reader stream);
    public abstract Expression TopLevelExpression() throws ParseException;

    public Expression parse(String expression) throws ParseException {
        ReInit(new StringReader(expression));
        try {
            return TopLevelExpression();
        } catch (ParseException qpe) {
            ParseException e = new ParseException("Cannot parse expression '" + expression + "': " + qpe.getMessage());
            e.initCause(qpe);
            throw e;
        }
    }

}
