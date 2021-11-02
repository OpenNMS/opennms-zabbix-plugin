package org.opennms.plugins.zabbix.shell;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.integration.api.v1.config.datacollection.graphs.PrefabGraph;
import org.opennms.plugins.zabbix.TemplateResolver;
import org.opennms.plugins.zabbix.config.GraphPropertiesExtension;

@Command(scope = "opennms-zabbix", name = "dump-graphs", description="Dump all graphs")
@Service
public class DumpGraphs  implements Action {
    @Reference
    private TemplateResolver templateResolver;

    @Override
    public Object execute() {
        GraphPropertiesExtension graphPropertiesExtension = new GraphPropertiesExtension(templateResolver.getZabbixTemplateHandler());
        List<PrefabGraph> graphs = graphPropertiesExtension.getPrefabGraphs();

        System.out.print("reports=");
        boolean first=true;
        for (PrefabGraph graph : graphs) {
            if (!first) {
                System.out.print(", \\\n");
            } else {
                first = false;
            }
            System.out.print(graph.getName());
        }

        for (PrefabGraph graph : graphs) {
            System.out.printf("report.%s.name=%s\n", graph.getName(), graph.getTitle());
            System.out.printf("report.%s.columns=%s\n",  graph.getName(), String.join(",", graph.getColumns()));
            System.out.printf("report.%s.type=%s\n", graph.getName(), String.join(",", graph.getTypes()));
            System.out.printf("report.%s.command=%s\n", graph.getName(), graph.getCommand());
            System.out.println();
        }

        return null;
    }
}
