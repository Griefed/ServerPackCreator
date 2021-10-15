package de.griefed.serverpackcreator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component
public class ApplicationProperties extends Properties {

    private final Logger LOG = LogManager.getLogger(ApplicationProperties.class);

    @Autowired
    public ApplicationProperties() {
        try (
        InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            new Properties();
            load(inputStream);
        } catch (
        IOException ex) {
            LOG.error("Couldn't read properties file.", ex);
        }
    }

}
