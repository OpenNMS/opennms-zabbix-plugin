package org.opennms.plugins.zabbix.shell;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.plugins.zabbix.TemplateResolver;
import org.opennms.plugins.zabbix.model.Template;

@Command(scope = "opennms-zabbix", name = "get-templates", description="Retrieve the templates used for a given node.")
@Service
public class GetTemplates implements Action {

    @Reference
    private TemplateResolver templateResolver;

    @Option(name = "-n", aliases = "--node", description = "Node criteria (id or fs:fid)", required = true)
    private String nodeCriteria;

    @Override
    public Object execute() {
        List<Template> templates = templateResolver.getTemplatesForNode(nodeCriteria);
        if (templates.isEmpty()) {
            System.out.println("No associated templates found for node: " + nodeCriteria);
        }
        for (Template template : templates) {
            System.out.println(" - " + template.getName());
        }
        return null;
    }
}
