package org.opennms.plugins.zabbix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
        try {
            for (String path : getResourceFiles("templates/")) {
                InputStream inputStream = this.getClass()
                        .getClassLoader()
                        .getResourceAsStream(path);
                Map<String, Object> obj = yaml.load(inputStream);
                templates.add(obj);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return templates;
    }


    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        try (InputStream in = getResourceAsStream(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                filenames.add(path + resource);
            }
        }
        return filenames;
    }

    private InputStream getResourceAsStream(String resource) {
        final InputStream in = getContextClassLoader().getResourceAsStream(resource);
        return in == null ? getClass().getResourceAsStream(resource) : in;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

}
