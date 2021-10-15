package org.opennms.plugins.zabbix;

import java.io.InputStream;
import java.util.Map;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

public class ZabbixTemplateTest {

    @Test
    public void canLoadTemplate() {
        final Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("templates/template_os_windows_agent.yaml");
        Map<String, Object> obj = yaml.load(inputStream);
    }
}
