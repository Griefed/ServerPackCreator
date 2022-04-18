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
        return APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_MINECRAFT;
    }

    @Bean
    public File forgeManifest() {
        return APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FORGE;
    }

    @Bean
    public File fabricManifest() {
        return APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC;
    }

    @Bean
    public File fabricInstallerManifest() {
        return APPLICATIONPROPERTIES.PATH_FILE_MANIFEST_FABRIC_INSTALLER;
    }
}
