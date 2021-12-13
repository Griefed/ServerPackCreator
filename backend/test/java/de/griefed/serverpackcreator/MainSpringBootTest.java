/* Copyright (C) 2021  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import java.io.File;
import java.io.IOException;

@SpringBootTest(classes = MainSpringBootTest.class)
@PropertySources({
        @PropertySource("classpath:application.properties")
})
public class MainSpringBootTest {

    private final DefaultFiles DEFAULTFILES;
    private final LocalizationManager LOCALIZATIONMANAGER;
    private ApplicationProperties serverPackCreatorProperties;

    MainSpringBootTest() {
        try {
            FileUtils.copyFile(new File("backend/main/resources/serverpackcreator.properties"),new File("serverpackcreator.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileUtils.copyFile(new File("backend/test/resources/serverpackcreator.db"),new File("serverpackcreator.db"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.serverPackCreatorProperties = new ApplicationProperties();

        LOCALIZATIONMANAGER = new LocalizationManager(serverPackCreatorProperties);
        LOCALIZATIONMANAGER.init();
        DEFAULTFILES = new DefaultFiles(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        DEFAULTFILES.filesSetup();
        DEFAULTFILES.checkDatabase();
    }

    @Test
    void contextLoads() {

    }
}
