package org.opennms.plugins.zabbix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.opennms.plugins.zabbix.model.DiscoveryRule;
import org.opennms.plugins.zabbix.model.Item;
import org.opennms.plugins.zabbix.model.Template;
import org.opennms.plugins.zabbix.model.TemplateMeta;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ZabbixTemplateHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ZabbixTemplateHandler.class);

    private final ObjectMapper om;
    private final BundleContext bundleContext;

    public ZabbixTemplateHandler() {
        this(null);
    }

    public ZabbixTemplateHandler(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        om = new ObjectMapper(new YAMLFactory());
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Set<String> getDiscoveryKeys(Template template) {
        final Set<String> discoveryKeys = new LinkedHashSet<>();
        for (DiscoveryRule rule : template.getDiscoveryRules()) {
            discoveryKeys.add(rule.getKey());
        }
        return discoveryKeys;
    }

    public Set<String> getDiscoveryKeys() {
        final Set<String> discoveryKeys = new LinkedHashSet<>();

        final List<TemplateMeta> allTemplates = loadTemplates();
        for (TemplateMeta templateMeta : allTemplates) {
            for (Template template : templateMeta.getZabbixExport().getTemplates()) {
                for (DiscoveryRule rule : template.getDiscoveryRules()) {
                    discoveryKeys.add(rule.getKey());
                }
            }
        }

        return discoveryKeys;
    }

    public List<String> getKeys(Template template) {
        final List<String> keys = new ArrayList<>();
        for (Item item : template.getItems()) {
            keys.add(item.getKey());
        }
        return keys;
    }

    public List<String> getKeys() {
        final List<String> keys = new ArrayList<>();

        final List<TemplateMeta> allTemplates = loadTemplates();
        for (TemplateMeta templateMeta : allTemplates) {
            for (Template template : templateMeta.getZabbixExport().getTemplates()) {
                for (Item item : template.getItems()) {
                    keys.add(item.getKey());
                }
            }
        }

        return keys;
    }

    public List<TemplateMeta> loadTemplates() {
        final List<TemplateMeta> templates = new LinkedList<>();
        if (bundleContext == null) {
            try {
                for (String path : getResourceFiles("templates/")) {
                    try (InputStream inputStream = getResourceAsStream(path)) {
                        templates.add(om.readValue(inputStream, TemplateMeta.class));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            final Enumeration<URL> ee = bundleContext.getBundle().findEntries("templates", "*.yaml", false);
            while (ee.hasMoreElements()) {
                final URL url = ee.nextElement();
                try (InputStream inputStream = url.openStream()) {
                    templates.add(om.readValue(inputStream, TemplateMeta.class));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return templates;
    }

    public List<Template> getTemplates() {
        return loadTemplates().stream()
                .map(TemplateMeta::getZabbixExport)
                .flatMap(export -> export.getTemplates().stream())
                .collect(Collectors.toList());
    }

    public Optional<Template> getTemplateByName(String name) {
        return loadTemplates().stream()
                .map(TemplateMeta::getZabbixExport)
                .flatMap(export -> export.getTemplates().stream())
                .filter(t -> Objects.equals(name, t.getName()))
                .findFirst();
    }

    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        try (InputStream in = getResourceAsStream(path)) {
            if (in == null) {
                LOG.warn("No resources found at path: {}", path);
                return Collections.emptyList();
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String resource;
                while ((resource = br.readLine()) != null) {
                    if (!resource.endsWith(".yaml")) {
                        continue;
                    }
                    filenames.add(path + resource);
                }
            }
        }
        return filenames;
    }

    private InputStream getResourceAsStream(String resource) throws IOException {
        InputStream in = getContextClassLoader().getResourceAsStream(resource);
        return in == null ? ZabbixTemplateHandler.class.getResourceAsStream(resource) : in;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }



}
