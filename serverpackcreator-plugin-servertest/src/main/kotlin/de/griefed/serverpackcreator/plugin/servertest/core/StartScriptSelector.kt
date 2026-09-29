/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.plugin.servertest.core

import java.io.File

/**
 * The two families of host this plugin runs on, used only to pick a sensible **default** script.
 *
 * It decides nothing else. Which script actually runs is the user's choice, because the plugin guessing
 * wrong is the one failure that leaves somebody unable to start a pack at all.
 */
enum class Platform {
    /** Windows, where a pack is launched through its batch shim by default. */
    WINDOWS,

    /** Linux and macOS — anything that is not Windows, where `bash start.sh` is the default. */
    POSIX;

    companion object {
        /**
         * The family [osName] belongs to. Defaults to the running host, and takes the name as an argument so
         * both families can be exercised on either one.
         */
        fun of(osName: String = System.getProperty("os.name") ?: ""): Platform =
            if (osName.lowercase().contains("win")) WINDOWS else POSIX
    }
}

/**
 * One start script a generated pack may carry, and how to run it.
 *
 * Built from a **template key** rather than enumerated, because the keys are ServerPackCreator's own:
 * `ApiProperties.startScriptTemplates` maps one key per script type to its template, and
 * `ServerPackProvisioner` writes each out as `start.<key>`. Reading the same setting is what keeps this
 * dropdown from offering a script no generation produces, or omitting one an operator added.
 *
 * @author Griefed
 */
data class StartScript(
    /** The template key, which is also the generated script's extension — `sh`, `bat`, `ps1`, `fish`, … */
    val key: String,

    /** What the dropdown shows: the file, plus who it is for. */
    val label: String,

    /** The argv to spawn. The script is named relatively, so the working directory decides which pack. */
    val command: List<String>
) {
    /** The file inside a server pack, which `ServerPackProvisioner` names `start.<key>`. */
    val fileName: String get() = "$FILE_PREFIX$key"

    companion object {
        /** The prefix `ServerPackProvisioner.startScriptName` gives every generated start script. */
        const val FILE_PREFIX = "start."
    }
}

/**
 * Turns ServerPackCreator's configured start-script templates into the choices the user is offered.
 *
 * @author Griefed
 */
object StartScripts {

    /**
     * Shown when ServerPackCreator has no start-script templates configured at all.
     *
     * Reachable rather than defensive: the templates are a user-editable setting, and emptying it means
     * generations produce no start scripts, so there is genuinely nothing this plugin could run.
     */
    const val NO_SCRIPTS_CONFIGURED =
        "No start-script templates are configured in ServerPackCreator's settings, so no server pack has " +
                "a script to run."

    /**
     * How to run each script type ServerPackCreator ships a template for, and what to call it.
     *
     * Keyed on the same strings `ScriptTemplatesConfig.defaultStartScriptTemplates` uses. A key that is not
     * here is still offered — see [forKey] — because an operator may add a template for a shell this map
     * has never heard of, and refusing to list it would be the plugin overruling their configuration.
     */
    private val known: Map<String, Pair<String, List<String>>> = mapOf(
        "sh" to ("Linux / macOS (bash)" to listOf("bash")),
        "bat" to ("Windows (recommended)" to listOf("cmd", "/c")),
        "ps1" to ("Windows (PowerShell directly)" to listOf(
            "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File"
        )),
        "fish" to ("fish shell" to listOf("fish"))
    )

    /**
     * The choice for template key [key].
     *
     * An unknown key is executed **directly** rather than guessed at: ServerPackCreator marks every
     * generated start script executable, so a custom template carrying a shebang runs on its own. Guessing
     * an interpreter for a key nobody documented would be the same mistake the platform fallback made.
     */
    fun forKey(key: String): StartScript {
        val fileName = StartScript.FILE_PREFIX + key
        val interpreter = known[key.lowercase()]
        return StartScript(
            key = key,
            label = if (interpreter == null) "$fileName — run directly" else "$fileName — ${interpreter.first}",
            command = interpreter?.second?.plus(fileName) ?: listOf("./$fileName")
        )
    }

    /**
     * The choices for [templateKeys], in a stable order so the dropdown does not reshuffle between reads.
     *
     * The keys come straight from `ApiProperties.startScriptTemplates`, which is a `HashMap` and therefore
     * has no order of its own — the known types first, in the order a user is likely to want them, and
     * anything an operator added after, alphabetically.
     */
    fun forTemplateKeys(templateKeys: Collection<String>): List<StartScript> {
        val preferred = known.keys.toList()
        return templateKeys.distinct()
            .sortedWith(compareBy({ preferred.indexOf(it.lowercase()).takeIf { i -> i >= 0 } ?: preferred.size }, { it }))
            .map(::forKey)
    }

    /**
     * Which of [available] to pre-select on [platform]: the batch shim on Windows, bash elsewhere.
     *
     * Falls back to whatever is offered when the preferred key is not configured, and to `null` when
     * nothing is — an operator *can* configure no start scripts at all, and a dropdown with no entries is
     * a better answer than one lying about a script that will never exist.
     */
    fun defaultFor(available: List<StartScript>, platform: Platform = Platform.of()): StartScript? {
        val preferred = when (platform) {
            Platform.WINDOWS -> "bat"
            Platform.POSIX -> "sh"
        }
        return available.firstOrNull { it.key.equals(preferred, ignoreCase = true) } ?: available.firstOrNull()
    }
}

/**
 * Whether the chosen script is actually in the pack, and what to run if it is.
 *
 * A sealed pair rather than a nullable script plus a nullable reason: exactly one of the two is always
 * meaningful, and a type that says so cannot be read the wrong way round.
 */
sealed interface StartScriptSelection {

    /** The pack has the chosen script. [command] is the argv, relative to the pack directory. */
    data class Available(
        /** The script that will be run, as an absolute file — for the UI to name and for a final check. */
        val script: File,
        /** The argv to spawn, with the script named relatively so the working directory decides which pack. */
        val command: List<String>
    ) : StartScriptSelection

    /** The pack does not carry the chosen script. [reason] names the file that is missing. */
    data class Missing(
        /** Why nothing can be launched, in words the pack list can show a user. */
        val reason: String
    ) : StartScriptSelection
}

/**
 * Answers whether a given pack can be launched with a given script.
 *
 * Pure: which scripts a pack carries is read once, when the pack is discovered, so changing the dropdown
 * re-decides every row without touching the disk again.
 *
 * @author Griefed
 */
object StartScriptSelector {

    /** Whether [pack] carries [script], and what to run. */
    fun selectFor(pack: LaunchablePack, script: StartScript): StartScriptSelection =
        if (script.key in pack.scriptKeysPresent) {
            StartScriptSelection.Available(File(pack.directory, script.fileName), script.command)
        } else {
            StartScriptSelection.Missing(
                "This server pack has no ${script.fileName}. Pick another start script, or regenerate the " +
                        "pack with that template configured."
            )
        }

    /**
     * The template keys [packDirectory] actually carries a `start.<key>` for.
     *
     * Every `start.*` regular file, deliberately — not only the configured ones. What a pack *has* is a
     * fact about the directory, and keeping it independent of the settings means an older pack generated
     * under a different template set still reports itself honestly. `isFile`, so a directory named
     * `start.sh` is reported absent instead of failing at spawn time with a shell error.
     */
    fun scriptKeysIn(packDirectory: File): Set<String> =
        (packDirectory.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.startsWith(StartScript.FILE_PREFIX) }
            .mapNotNull { it.name.removePrefix(StartScript.FILE_PREFIX).takeIf(String::isNotEmpty) }
            .toSet()
}
