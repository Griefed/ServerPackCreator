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
package de.griefed.serverpackcreator;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Check the passed commandline-arguments with which ServerPackCreator was started and return the mode in which to run.
 *
 * @author Griefed
 */
public class CommandlineParser {

    /**
     * The mode in which ServerPackCreator will run in after the commandline arguments have been parsed and checked.
     */
    private final Mode MODE;
    /**
     * The language ServerPackCreator should use if any was specified. Null if none was specified, so we can use the
     * default language <code>en_us</code>.
     */
    private final String LANG;

    /**
     * Create a new CommandlineParser from the passed commandline-arguments with which ServerPackCreator was started.
     * The mode and language in which ServerPackCreator should run will thus be determined and available to you via
     * {@link #getModeToRunIn()} and {@link #getLanguageToUse()}.<br>
     * {@link #getLanguageToUse()} is wrapped in an {@link Optional} to quickly determine whether a language was specified.
     *
     * @param args {@link String}-array of commandline-arguments with which ServerPackCreator was started. Typically passed from {@link Main}.
     * @author Griefed
     */
    public CommandlineParser(String[] args) {

        List<String> argsList = Arrays.asList(args);

        /*
         * Check whether a language locale was specified by the user.
         * If none was specified, set LANG to null so the Optional returns false for isPresent(),
         * thus allowing us to use the locale set in the ApplicationProperties later on.
         */
        if (argsList.contains(Mode.LANG.argument())) {
            this.LANG = argsList.get(argsList.indexOf(Mode.LANG.argument()) + 1);
        } else {
            this.LANG = null;
        }

        /*
         * Check whether the user wanted us to print the help-text.
         */
        if (argsList.contains(Mode.HELP.argument())) {
            this.MODE = Mode.HELP;
            return;
        }

        /*
         * Check whether the user wants to check for update availability.
         */
        if (argsList.contains(Mode.UPDATE.argument())) {
            this.MODE = Mode.UPDATE;
            return;
        }

        /*
         * Check whether the user wants to generate a new serverpackcreator.conf from the commandline.
         */
        if (argsList.contains(Mode.CGEN.argument())) {
            this.MODE = Mode.CGEN;
            return;
        }

        /*
         * Check whether the user wants to run in commandline-mode or whether a GUI would not be supported.
         */
        if (argsList.contains(Mode.CLI.argument())) {
            this.MODE = Mode.CLI;
            return;
        } else if (GraphicsEnvironment.isHeadless()) {
            this.MODE = Mode.CLI;
            return;
        }

        /*
         * Check whether the user wants ServerPackCreator to run as a webservice.
         */
        if (argsList.contains(Mode.WEB.argument())) {
            this.MODE = Mode.WEB;
            return;
        }

        /*
         * Check whether the user wants to use ServerPackCreators GUI.
         */
        if (argsList.contains(Mode.GUI.argument())) {
            this.MODE = Mode.GUI;
            return;
        }

        /*
         * Check whether the user wants to set up and prepare the environment for subsequent runs.
         */
        if (argsList.contains(Mode.SETUP.argument())) {
            this.MODE = Mode.SETUP;
            return;
        }

        /*
         * Last but not least, failsafe-check whether a GUI would be supported.
         */
        if (!GraphicsEnvironment.isHeadless()) {
            this.MODE = Mode.GUI;
            return;
        }

        /*
         * If all else fails, exit ServerPackCreator.
         */
        this.MODE = Mode.EXIT;
    }

    /**
     * Get the mode in which ServerPackCreator should be run in.
     *
     * @return {@link Mode} in which ServerPackCreator should be run in.
     * @author Griefed
     */
    protected Mode getModeToRunIn() {
        return MODE;
    }

    /**
     * Get the locale in which ServerPackCreator should be run in, wrapped in an {@link Optional}.
     *
     * @return {@link String} The locale in which ServerPackCreator should be run in, wrapped in an {@link Optional}.
     * @author Griefed
     */
    protected Optional<String> getLanguageToUse() {
        return Optional.ofNullable(LANG);
    }

    /**
     * Mode-priorities. Highest to lowest.
     */
    public enum Mode {

        /**
         * Priority 0.
         * Print ServerPackCreators help to commandline.
         */
        HELP("-help"),

        /**
         * Priority 1.
         * Check whether a newer version of ServerPackCreator is available.
         */
        UPDATE("-update"),

        /**
         * Priority 2.
         * Run ServerPackCreators configuration generation.
         */
        CGEN("-cgen"),

        /**
         * Priority 3.
         * Run ServerPackCreator in commandline-mode. If no graphical environment is supported, this is the default
         * ServerPackCreator will enter, even when starting ServerPackCreator with no extra arguments at all.
         */
        CLI("-cli"),

        /**
         * Priority 4.
         * Run ServerPackCreator as a webservice.
         */
        WEB("-web"),

        /**
         * Priority 5.
         * Run ServerPackCreator with our GUI. If a graphical environment is supported, this is the default
         * ServerPackCreator will enter, even when starting ServerPackCreator with no extra arguments at all.
         */
        GUI("-gui"),

        /**
         * Priority 6
         * Set up and prepare the environment for subsequent runs of ServerPackCreator. This will create/copy all files
         * needed for ServerPackCreator to function properly from inside its JAR-file and setup everything else, too.
         */
        SETUP("--setup"),

        /**
         * Priority 7.
         * Exit ServerPackCreator.
         */
        EXIT("exit"),

        /**
         * Used when the user wants to change the language of ServerPackCreator.
         */
        LANG("-lang");

        private final String ARGUMENT;

        Mode(String cliArg) {
            this.ARGUMENT = cliArg;
        }

        /**
         * Textual representation of this mode.
         *
         * @return {@link String} Textual representation of this mode.
         * @author Griefed
         */
        public String argument() {
            return ARGUMENT;
        }
    }
}
