package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ZabbixMacroSupportTest {

    @Test
    public void canTestStringForMacro() {
        assertThat(ZabbixMacroSupport.containsMacro(null), equalTo(false));
        assertThat(ZabbixMacroSupport.containsMacro(""), equalTo(false));
        assertThat(ZabbixMacroSupport.containsMacro("wow"), equalTo(false));
        assertThat(ZabbixMacroSupport.containsMacro("{wow"), equalTo(false));
        assertThat(ZabbixMacroSupport.containsMacro("wow}"), equalTo(false));
        assertThat(ZabbixMacroSupport.containsMacro("{wow}"), equalTo(true));
        assertThat(ZabbixMacroSupport.containsMacro("{#FSNAME}"), equalTo(true));
        assertThat(ZabbixMacroSupport.containsMacro("\"{#FSNAME}\""), equalTo(true));
        assertThat(ZabbixMacroSupport.containsMacro("  {#FSNAME} something something"), equalTo(true));
    }

    @Test
    public void canEvaluateMacroWithContext() {
        assertThat(ZabbixMacroSupport.evaluateMacro("{#FSNAME}!",
                ImmutableMap.of("{#FSNAME}", "oops")), equalTo("oops!"));
    }

    @Test
    public void canFindMacrosInKey() {
        assertThat(ZabbixMacroSupport.getMacros(null), hasSize(0));
        assertThat(ZabbixMacroSupport.getMacros(""), hasSize(0));
        assertThat(ZabbixMacroSupport.getMacros("what?"), hasSize(0));
        assertThat(ZabbixMacroSupport.getMacros("{#FSNAME}"), hasSize(1));
        assertThat(ZabbixMacroSupport.getMacros("{#FSNAME {#FSNAME}"), hasSize(1));
        assertThat(ZabbixMacroSupport.getMacros("{#FSNAME} {#DISKNAME} {#DEVNAME}"), hasSize(3));
    }

    @Test
    public void canConvertMacrosToVariables() {
        // base cases, no macros
        assertThat(ZabbixMacroSupport.macrosToVariables(null), equalTo(null));
        assertThat(ZabbixMacroSupport.macrosToVariables(""), equalTo(""));
        assertThat(ZabbixMacroSupport.macrosToVariables("why?"), equalTo("why?"));
        // basic replacement
        assertThat(ZabbixMacroSupport.macrosToVariables("{#FSNAME}"), equalTo("${FSNAME}"));
        // incomplete + complete
        assertThat(ZabbixMacroSupport.macrosToVariables("{#FSNAME {#FSNAME}"), equalTo("{#FSNAME ${FSNAME}"));
        // multiple replacement
        assertThat(ZabbixMacroSupport.macrosToVariables("{#FSNAME} {#DISKNAME} {#DEVNAME}"),
                equalTo("${FSNAME} ${DISKNAME} ${DEVNAME}"));
    }
}
