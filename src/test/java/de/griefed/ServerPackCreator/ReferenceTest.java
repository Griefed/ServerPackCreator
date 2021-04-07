package de.griefed.ServerPackCreator;

import com.typesafe.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.List;

class ReferenceTest {
    @Mock
    File oldConfigFile;
    @Mock
    File configFile;
    @Mock
    File propertiesFile;
    @Mock
    File iconFile;
    @Mock
    File forgeWindowsFile;
    @Mock
    File forgeLinuxFile;
    @Mock
    File fabricWindowsFile;
    @Mock
    File fabricLinuxFile;
    @Mock
    Config conf;
    @Mock
    List<String> clientMods;
    @Mock
    List<String> copyDirs;
    @InjectMocks
    Reference reference;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme