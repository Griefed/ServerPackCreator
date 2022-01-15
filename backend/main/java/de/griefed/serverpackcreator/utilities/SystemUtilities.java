/* Copyright (C) 2022  Griefed
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
package de.griefed.serverpackcreator.utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility-class revolving around the system we are running on.
 * @author Griefed
 */
@Component
public class SystemUtilities {

    private static final Logger LOG = LogManager.getLogger(SystemUtilities.class);

    private final List<String> DRIVES = new ArrayList<>(Arrays.asList(
            "A:","B:","C:","D:","E:","F:","G:","H:","I:","J:","K:","L:","M:","N:","O:","P:","Q:","R:","S:","T:","U:","V:","W:","X:","Y:","Z:"
    ));

    @Autowired
    public SystemUtilities() {

    }

    /**
     * Automatically acquire the path to the systems default Java installation.
     * @author Griefed
     * @return String. The path to the systems default Java installation.
     */
    public String acquireJavaPathFromSystem() {

        LOG.debug("Acquiring path to Java installation from system properties...");

        String javaPath = "Couldn't acquire JavaPath";

        if (new File(System.getProperty("java.home")).exists()) {
            javaPath = String.format("%s/bin/java",System.getProperty("java.home").replace("\\", "/"));

            if (!javaPath.startsWith("/")) {
                for (String letter : DRIVES) {
                    if (javaPath.startsWith(letter)) {

                        LOG.debug("We're running on Windows. Ensuring javaPath ends with .exe");
                        javaPath = String.format("%s.exe", javaPath);
                    }
                }
            }
        }

        return javaPath;
    }
}
