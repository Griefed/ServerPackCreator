package de.griefed.serverpackcreator.spring;

import de.griefed.serverpackcreator.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class BeanConfiguration {

    private final ApplicationProperties APPLICATIONPROPERTIES;

    @Autowired
    public BeanConfiguration(ApplicationProperties applicationProperties) {
        this.APPLICATIONPROPERTIES = applicationProperties;
    }

    @Bean
    public File minecraftManifest() {
        return APPLICATIONPROPERTIES.MINECRAFT_VERSION_MANIFEST_LOCATION();
    }

    @Bean
    public File forgeManifest() {
        return APPLICATIONPROPERTIES.FORGE_VERSION_MANIFEST_LOCATION();
    }

    @Bean
    public File fabricManifest() {
        return APPLICATIONPROPERTIES.FABRIC_VERSION_MANIFEST_LOCATION();
    }

    @Bean
    public File fabricInstallerManifest() {
        return APPLICATIONPROPERTIES.FABRIC_INSTALLER_VERSION_MANIFEST_LOCATION();
    }
}
