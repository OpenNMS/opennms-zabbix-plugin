package org.opennms.plugins.zabbix;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class ZabbixTemplateHandler {


    public List<String> getKeys() {
        final List<String> keys = new ArrayList<>();

        final List<Map<String, Object>> allTemplates = loadTemplates();
        for (Map<String, Object> thisTemplate : allTemplates) {
            Map<String, Object> export = (Map<String, Object>)thisTemplate.get("zabbix_export");
            List<Map<String,Object>> templates = (List<Map<String,Object>>)export.get("templates");
            for (Map<String,Object> template : templates) {

                List<Map<String,Object>> items = (List<Map<String,Object>>)template.get("items");
                if (items == null) {
                    continue;
                }
                for (Map<String,Object> item : items) {
                    String key = (String)item.get("key");
                    keys.add(key);
                }
            }
        }

        return keys;
    }

    public List<Map<String, Object>> loadTemplates() {
        final List<Map<String, Object>> templates = new LinkedList<>();


        final Yaml yaml = new Yaml();
        for (String path : Arrays.asList("templates/template_os_windows_agent.yaml", "templates/template_os_linux.yaml")) {
            InputStream inputStream = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream(path);
            Map<String, Object> obj = yaml.load(inputStream);
            templates.add(obj);
        }
        return templates;
    }

}
