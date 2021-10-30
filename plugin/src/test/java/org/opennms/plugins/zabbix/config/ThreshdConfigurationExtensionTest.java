package org.opennms.plugins.zabbix.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import org.junit.Test;

public class ThreshdConfigurationExtensionTest {

    @Test
    public void canLoad() {
        ThreshdConfigurationExtension threshdConfigurationExtension = new ThreshdConfigurationExtension();
        assertThat(threshdConfigurationExtension.getPackages(), not(empty()));
    }

}
