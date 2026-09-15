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
package de.griefed.serverpackcreator.app.web

import com.electronwill.nightconfig.toml.TomlParser
import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.api.modscanning.*
import de.griefed.serverpackcreator.api.serverpack.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.JsonUtilities
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.api.utilities.common.XmlUtilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory


/**
 * Bean configuration for running ServerPackCreator as a webservice. This class provides beans for a
 * couple or properties which can not otherwise be provided.
 *
 * @author Griefed
 */
@Suppress("unused")
@Configuration
class BeanConfiguration {

    /**
     * The one [ApiWrapper] the whole web application shares, and the root every other bean here reads from.
     * Beware: this method is *called* by the beans below rather than injected into them. That works — and returns this
     * same singleton rather than building a second wrapper — only because `@Configuration` is proxied by default. Do
     * not set `proxyBeanMethods = false` here without rewriting those call sites into parameters.
     */
    @Bean
    fun apiWrapper(): ApiWrapper {
        return ApiWrapper.api()
    }

    /** Exposes the API's own settings object as a bean, for injection into the web layer. */
    @Bean
    fun applicationProperties(): ApiProperties {
        return apiWrapper().apiProperties
    }

    /** Exposes the API's utility aggregate as a bean, for injection into the web layer. */
    @Bean
    fun utilities(): Utilities {
        return apiWrapper().utilities
    }

    /** Exposes the Forge annotation scanner (Minecraft 1.12 and older) as a bean, for injection into the web layer. */
    @Bean
    fun annotationScanner(): ForgeAnnotationScanner {
        return apiWrapper().forgeAnnotationScanner
    }

    /** Exposes the loaded pf4j plugins as a bean, for injection into the web layer. */
    @Bean
    fun applicationPlugins(): ApiPlugins {
        return apiWrapper().apiPlugins
    }

    /** Exposes the configuration validator as a bean, for injection into the web layer. */
    @Bean
    fun configurationHandler(): ConfigurationHandler {
        return apiWrapper().configurationHandler
    }

    /** Exposes the Fabric mod scanner as a bean, for injection into the web layer. */
    @Bean
    fun fabricScanner(): FabricScanner {
        return apiWrapper().fabricScanner
    }

    /** Exposes the JSON helpers as a bean, for injection into the web layer. */
    @Bean
    fun jsonUtilities(): JsonUtilities {
        return apiWrapper().jsonUtilities
    }

    /** Exposes the loader-dispatching mod scanner as a bean, for injection into the web layer. */
    @Bean
    fun modScanner(): ModScanner {
        return apiWrapper().modScanner
    }

    /** Exposes the Quilt mod scanner as a bean, for injection into the web layer. */
    @Bean
    fun quiltScanner(): QuiltScanner {
        return apiWrapper().quiltScanner
    }

    /** Exposes the server-pack generator as a bean, for injection into the web layer. */
    @Bean
    fun serverPackHandler(): ServerPackHandler {
        return apiWrapper().serverPackHandler
    }

    /** Exposes the Forge `mods.toml` scanner (Minecraft 1.13 and newer) as a bean, for injection into the web layer. */
    @Bean
    fun ForgeTomlScanner(): ForgeTomlScanner {
        return apiWrapper().forgeTomlScanner
    }

    /** Exposes the NeoForge `mods.toml` scanner, which reads the same descriptor from a moved path as a bean, for injection into the web layer. */
    @Bean
    fun NeoForgeTomlScanner(): NeoForgeTomlScanner {
        return apiWrapper().neoForgeTomlScanner
    }

    /** Exposes the version manifests as a bean, for injection into the web layer. */
    @Bean
    fun versionMeta(): VersionMeta {
        return apiWrapper().versionMeta
    }

    /** Exposes the HTTP helpers as a bean, for injection into the web layer. */
    @Bean
    fun webUtilities(): WebUtilities {
        return apiWrapper().webUtilities
    }

    /** Exposes the XML helpers as a bean, for injection into the web layer. */
    @Bean
    fun xmlUtilities(): XmlUtilities {
        return apiWrapper().xmlUtilities
    }

    /** Exposes the cached Minecraft version manifest file as a bean, for injection into the web layer. */
    @Bean
    fun minecraftManifest(): File {
        return apiWrapper().apiProperties.minecraftVersionManifest
    }

    /** Exposes the cached Forge version manifest file as a bean, for injection into the web layer. */
    @Bean
    fun forgeManifest(): File {
        return apiWrapper().apiProperties.forgeVersionManifest
    }

    /** Exposes the cached NeoForge version manifest file as a bean, for injection into the web layer. */
    @Bean
    fun neoForgeManifest(): File {
        return apiWrapper().apiProperties.oldNeoForgeVersionManifest
    }

    /** Exposes the cached Fabric loader manifest file as a bean, for injection into the web layer. */
    @Bean
    fun fabricManifest(): File {
        return apiWrapper().apiProperties.fabricVersionManifest
    }

    /** Exposes the cached Fabric intermediaries manifest file as a bean, for injection into the web layer. */
    @Bean
    fun fabricIntermediariesManifest(): File {
        return apiWrapper().apiProperties.fabricIntermediariesManifest
    }

    /** Exposes the cached Fabric installer manifest file as a bean, for injection into the web layer. */
    @Bean
    fun fabricInstallerManifest(): File {
        return apiWrapper().apiProperties.fabricInstallerManifest
    }

    /** Exposes the cached Quilt loader manifest file as a bean, for injection into the web layer. */
    @Bean
    fun quiltManifest(): File {
        return apiWrapper().apiProperties.quiltVersionManifest
    }

    /** Exposes the cached Quilt installer manifest file as a bean, for injection into the web layer. */
    @Bean
    fun quiltInstallerManifest(): File {
        return apiWrapper().apiProperties.quiltInstallerManifest
    }

    /** An empty argument array, so anything expecting command-line args injected can be constructed in the web context. */
    @Bean
    fun args(): Array<String?> {
        return arrayOfNulls(0)
    }

    /** Exposes the API's configured Jackson mapper, so the web layer serialises the way the API does as a bean, for injection into the web layer. */
    @Bean
    fun objectMapper(): ObjectMapper {
        return apiWrapper().objectMapper
    }

    /** Exposes the TOML parser as a bean, for injection into the web layer. */
    @Bean
    fun tomlParser(): TomlParser {
        return apiWrapper().tomlParser
    }

    /** Exposes the cached LegacyFabric game manifest file as a bean, for injection into the web layer. */
    @Bean
    fun legacyFabricGameManifest(): File {
        return apiWrapper().apiProperties.legacyFabricGameManifest
    }

    /** Exposes the cached LegacyFabric loader manifest file as a bean, for injection into the web layer. */
    @Bean
    fun legacyFabricLoaderManifest(): File {
        return apiWrapper().apiProperties.legacyFabricLoaderManifest
    }

    /** Exposes the cached LegacyFabric installer manifest file as a bean, for injection into the web layer. */
    @Bean
    fun legacyFabricInstallerManifest(): File {
        return apiWrapper().apiProperties.legacyFabricInstallerManifest
    }

    /** Exposes the XML document-builder factory as a bean, for injection into the web layer. */
    @Bean
    fun documentBuilder(): DocumentBuilderFactory {
        return apiWrapper().documentBuilderFactory
    }

    /** A SHA-256 digest, the hash modpack de-duplication is keyed on. A fresh instance per injection, since a `MessageDigest` is stateful and not thread-safe. */
    @Bean
    fun messageDigestInstance(): MessageDigest {
        return MessageDigest.getInstance("SHA-256")
    }
}