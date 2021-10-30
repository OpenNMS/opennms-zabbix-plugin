package org.opennms.plugins.zabbix.config;

import java.util.List;

import org.opennms.integration.api.v1.config.thresholding.PackageDefinition;
import org.opennms.integration.api.xml.ClasspathThreshdConfigurationLoader;

public class ThreshdConfigurationExtension implements org.opennms.integration.api.v1.config.thresholding.ThreshdConfigurationExtension {
    private final ClasspathThreshdConfigurationLoader classpathThreshdConfigurationLoader =
            new ClasspathThreshdConfigurationLoader(ThreshdConfigurationExtension.class, "threshd-configuration.xml");

    @Override
    public List<PackageDefinition> getPackages() {
        return classpathThreshdConfigurationLoader.getPackages();
    }
}
