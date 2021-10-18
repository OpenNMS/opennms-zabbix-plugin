package org.opennms.plugins.zabbix;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.model.MetaData;
import org.opennms.integration.api.v1.model.Node;
import org.opennms.plugins.zabbix.model.LinkedTemplate;
import org.opennms.plugins.zabbix.model.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateResolver {
    public static final String ZABBIX_AGENT_TEMPLATE_NAME = "Zabbix agent";

    private static final Logger LOG = LoggerFactory.getLogger(TemplateResolver.class);

    private final NodeDao nodeDao;
    private final ZabbixTemplateHandler zabbixTemplateHandler;

    public TemplateResolver(NodeDao nodeDao, ZabbixTemplateHandler templateHandler) {
        this.nodeDao = Objects.requireNonNull(nodeDao);
        this.zabbixTemplateHandler = Objects.requireNonNull(templateHandler);
    }

    public List<Template> getTemplatesForNode(String nodeCriteria) {
        final Node node = nodeDao.getNodeByCriteria(nodeCriteria);
        if (node == null) {
            return resolveTemplates(Collections.singletonList(ZABBIX_AGENT_TEMPLATE_NAME));
        }

        final List<String> templateNamesInZabbixContext = node.getMetaData().stream()
                .filter(m -> "zabbix".equals(m.getContext()))
                .filter(m -> m.getKey().startsWith("template"))
                .sorted(Comparator.comparing(MetaData::getKey))
                .map(MetaData::getValue)
                .collect(Collectors.toList());
        final List<String> templateNamesInRequisitionContext = node.getMetaData().stream()
                .filter(m -> "requisition".equals(m.getContext()))
                .filter(m -> m.getKey().startsWith("zabbix.template"))
                .sorted(Comparator.comparing(MetaData::getKey))
                .map(MetaData::getValue)
                .collect(Collectors.toList());

        final Set<String> templateNames = new LinkedHashSet<>();
        templateNames.addAll(templateNamesInZabbixContext);
        templateNames.addAll(templateNamesInRequisitionContext);

        final List<Template> templates = new LinkedList<>();
        templateNames.forEach(name -> addTemplateTree(name, templates));

        if (templates.isEmpty()) {
            return resolveTemplates(Collections.singletonList(ZABBIX_AGENT_TEMPLATE_NAME));
        }
        return templates;
    }

    public List<Template> resolveTemplates(List<String> templateNames) {
        final List<Template> templates = new LinkedList<>();
        templateNames.forEach(name -> addTemplateTree(name, templates));
        return templates;
    }

    private void addTemplateTree(String name, List<Template> templates) {
        if (templates.stream().anyMatch(t -> Objects.equals(name, t.getName()))) {
            // ignore if template is already present
            return;
        }

        // Lookup by name
        final Template template = zabbixTemplateHandler.getTemplateByName(name).orElse(null);
        if (template == null) {
            LOG.warn("Template {} is referenced, but was not found.", name);
            return;
        }

        // Add
        templates.add(template);

        // Recurse
        for (LinkedTemplate linkedTemplate : template.getLinkedTemplates()) {
            addTemplateTree(linkedTemplate.getName(), templates);
        }
    }

}
