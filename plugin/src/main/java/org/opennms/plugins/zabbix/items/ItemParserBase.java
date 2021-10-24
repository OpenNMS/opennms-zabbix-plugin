package org.opennms.plugins.zabbix.items;

import java.io.Reader;
import java.io.StringReader;

import org.opennms.plugins.zabbix.expressions.ItemKey;

public abstract class ItemParserBase {

    // Generated functions
    public abstract void ReInit(Reader stream);
    public abstract ItemKey TopLevelItem() throws ParseException;

    public ItemKey parse(String item) throws ParseException {
        ReInit(new StringReader(item));
        try {
            return TopLevelItem();
        } catch (ParseException qpe) {
            ParseException e = new ParseException("Cannot parse item '" + item + "': " + qpe.getMessage());
            e.initCause(qpe);
            throw e;
        }
    }

}
