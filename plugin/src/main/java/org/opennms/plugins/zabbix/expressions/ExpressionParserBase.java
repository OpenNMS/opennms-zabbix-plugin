package org.opennms.plugins.zabbix.expressions;

import java.io.Reader;
import java.io.StringReader;

public abstract class ExpressionParserBase {

    // Generated functions
    public abstract void ReInit(Reader stream);
    public abstract Expression TopLevelExpression() throws ParseException;
    public abstract ItemKey TopLevelItem() throws ParseException;
    public abstract HostAndKey TopLevelHostAndKey() throws ParseException;

    public Expression parse(String input) throws ParseException {
        ReInit(new StringReader(input));
        try {
            return TopLevelExpression();
        } catch (ParseException qpe) {
            ParseException e = new ParseException("Cannot parse input '" + input + "': " + qpe.getMessage());
            e.initCause(qpe);
            throw e;
        }
    }

    public ItemKey parseItem(String input) throws ParseException {
        ReInit(new StringReader(input));
        try {
            return TopLevelItem();
        } catch (ParseException qpe) {
            ParseException e = new ParseException("Cannot parse input '" + input + "': " + qpe.getMessage());
            e.initCause(qpe);
            throw e;
        }
    }

    public HostAndKey parseHostAndKey(String input) throws ParseException {
        ReInit(new StringReader(input));
        try {
            return TopLevelHostAndKey();
        } catch (ParseException qpe) {
            ParseException e = new ParseException("Cannot parse input '" + input + "': " + qpe.getMessage());
            e.initCause(qpe);
            throw e;
        }
    }

}
