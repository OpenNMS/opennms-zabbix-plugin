package org.opennms.plugins.zabbix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.model.MetaData;
import org.opennms.integration.api.v1.model.Node;
import org.opennms.integration.api.v1.model.immutables.ImmutableMetaData;
import org.opennms.plugins.zabbix.model.Template;

public class TemplateResolverTest {

    @Test
    public void canGetTemplatesForNode() {
        List<MetaData> metaData = new LinkedList<>();
        metaData.add(ImmutableMetaData.newBuilder()
                .setContext("zabbix")
                .setKey("template")
                .setValue("Linux by Zabbix agent")
                .build());
        Node node = mock(Node.class);
        when(node.getMetaData()).thenReturn(metaData);
        NodeDao nodeDao = mock(NodeDao.class);
        when(nodeDao.getNodeByCriteria("1")).thenReturn(node);

        ZabbixTemplateHandler templateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = new TemplateResolver(nodeDao, templateHandler);
        List<Template> templates = templateResolver.getTemplatesForNode("1");

        List<String> templateNames = templates.stream()
                .map(Template::getName)
                .collect(Collectors.toList());

        assertThat(templateNames, contains(
                "Linux by Zabbix agent",
                "Linux block devices by Zabbix agent",
                "Linux CPU by Zabbix agent",
                "Linux filesystems by Zabbix agent",
                "Linux generic by Zabbix agent",
                "Linux memory by Zabbix agent",
                "Linux network interfaces by Zabbix agent",
                "Zabbix agent"));
    }

    @Test
    public void canLoadBasixAgentTemplateAsFallback() {
        NodeDao nodeDao = mock(NodeDao.class);
        ZabbixTemplateHandler templateHandler = new ZabbixTemplateHandler();
        TemplateResolver templateResolver = new TemplateResolver(nodeDao, templateHandler);
        List<Template> templates = templateResolver.getTemplatesForNode("not:exists");
        assertThat(templates, hasSize(1));
        assertThat(templates.get(0).getName(), equalTo("Zabbix agent"));
    }
}
