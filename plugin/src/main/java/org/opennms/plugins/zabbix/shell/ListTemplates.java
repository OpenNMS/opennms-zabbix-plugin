package org.opennms.plugins.zabbix.shell;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.plugins.zabbix.TemplateResolver;
import org.opennms.plugins.zabbix.model.LinkedTemplate;
import org.opennms.plugins.zabbix.model.Template;

@Command(scope = "opennms-zabbix", name = "list-templates", description="List all known templates.")
@Service
public class ListTemplates implements Action {

    @Reference
    private TemplateResolver templateResolver;

    @Override
    public Object execute() {
        List<Template> templates = templateResolver.getZabbixTemplateHandler().getTemplates();
        if (templates.isEmpty()) {
            System.out.println("No templates found!");
            return null;
        }
        System.out.println("Known templates:");
        for (Template template : templates) {
            System.out.println(" - " + template.getName());
            for (LinkedTemplate linkedTemplate : template.getLinkedTemplates()) {
                System.out.println("     - " + linkedTemplate.getName());
            }
        }
        return null;
    }
}
