package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.utilities.common.concatenate

/**
 * Conveniently access all different check-types, whether they passed and which errors, if any, were encountered.
 *
 * @author Griefed
 */
class ConfigCheck {

    val configErrors: MutableList<String> = mutableListOf()
    val configChecksPassed: Boolean
        get() {
            return configErrors.isEmpty()
        }

    val modpackErrors: MutableList<String> = mutableListOf()
    val modpackChecksPassed: Boolean
        get() {
            return modpackErrors.isEmpty()
        }

    val inclusionErrors: MutableList<String> = mutableListOf()
    val inclusionsChecksPassed: Boolean
        get() {
            return inclusionErrors.isEmpty()
        }

    val minecraftVersionErrors: MutableList<String> = mutableListOf()
    val minecraftVersionChecksPassed: Boolean
        get() {
            return minecraftVersionErrors.isEmpty()
        }

    val modloaderErrors: MutableList<String> = mutableListOf()
    val modloaderChecksPassed: Boolean
        get() {
            return modloaderErrors.isEmpty()
        }

    val modloaderVersionErrors: MutableList<String> = mutableListOf()
    val modloaderVersionChecksPassed: Boolean
        get() {
            return modloaderVersionErrors.isEmpty()
        }

    val serverIconErrors: MutableList<String> = mutableListOf()
    val serverIconChecksPassed: Boolean
        get() {
            return serverIconErrors.isEmpty()
        }

    val serverPropertiesErrors: MutableList<String> = mutableListOf()
    val serverPropertiesChecksPassed: Boolean
        get() {
            return serverPropertiesErrors.isEmpty()
        }

    val pluginsErrors: MutableList<String> = mutableListOf()
    val pluginsChecksPassed: Boolean
        get() {
            return pluginsErrors.isEmpty()
        }

    val otherErrors: MutableList<String> = mutableListOf()
    val otherChecksPassed: Boolean
        get() {
            return otherErrors.isEmpty()
        }

    val encounteredErrors: MutableList<String>
        get() {
            return concatenate(
                configErrors,
                modpackErrors,
                inclusionErrors,
                minecraftVersionErrors,
                modloaderErrors,
                modloaderVersionErrors,
                serverIconErrors,
                serverPropertiesErrors,
                pluginsErrors,
                otherErrors
            ).toMutableList()
        }
    val allChecksPassed: Boolean
        get() {
            return encounteredErrors.isEmpty()
        }

    /**
     * Merge with [check]
     *
     * @author Griefed
     */
    fun and(check: ConfigCheck): ConfigCheck {
        configErrors.addAll(check.configErrors)
        modpackErrors.addAll(check.modpackErrors)
        inclusionErrors.addAll(check.inclusionErrors)
        minecraftVersionErrors.addAll(check.minecraftVersionErrors)
        modloaderErrors.addAll(check.modloaderErrors)
        modloaderVersionErrors.addAll(check.modloaderVersionErrors)
        serverIconErrors.addAll(check.serverIconErrors)
        serverPropertiesErrors.addAll(check.serverPropertiesErrors)
        pluginsErrors.addAll(check.pluginsErrors)
        otherErrors.addAll(check.otherErrors)
        return this
    }
}