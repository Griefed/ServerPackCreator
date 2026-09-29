import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
    id("serverpackcreator.plugin-conventions")
    kotlin("kapt")
}

// The identity pf4j loads this plugin under. `pluginId` is the key its configuration would be stored
// under, so it must stay stable across releases even though this plugin stores none.
val pluginClass = "de.griefed.serverpackcreator.plugin.selfextract.SelfExtractPlugin"
val pluginId = "selfextract"
val pluginName = "Self-Extracting Server Packs"
val pluginDescription =
    "Wraps every generated server pack in a self-extracting script - one for Linux and macOS, one for Windows - that unpacks itself and starts the server."
val pluginAuthor = "Griefed"

dependencies {
    annotationProcessor(libs.pf4j)
    kapt(libs.pf4j)
    // -api only, like every other plugin here, and that constraint is what shapes this one. A pf4j
    // plugin jar carries no dependencies of its own -- nothing in this repository builds a fat plugin
    // jar -- and the host's runtime classpath has no TAR writer on it, so `core.TarGzWriter` writes
    // the format itself rather than pulling in commons-compress that could not be delivered anyway.
    implementation(project(":serverpackcreator-api"))

    // Testing
    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)
    // For the collaborators the extension is handed and never reads, so its guard can pin the real
    // `run` signature rather than a private helper the signature might stop calling.
    testImplementation(libs.mockk)
}

tasks.processResources {
    // Read into locals first: referencing the script's own properties from inside the closure would
    // capture the build script, which the configuration cache cannot serialize.
    val expansions = mapOf(
        "version" to project.version,
        "plugin_id" to pluginId,
        "plugin_name" to pluginName,
        "plugin_description" to pluginDescription,
        "plugin_author" to pluginAuthor
    )
    filesMatching("plugin.toml") {
        expand(expansions)
    }
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to pluginClass,
                "Description" to pluginDescription,
                "Built-By" to System.getProperty("user.name"),
                "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date()),
                "Created-By" to "Gradle ${gradle.gradleVersion}",
                "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${
                    System.getProperty(
                        "java.vm.version"
                    )
                })",
                "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}",
                "Plugin-Class" to pluginClass,
                "Plugin-Id" to pluginId,
                "Plugin-Name" to pluginName,
                "Plugin-Provider" to pluginAuthor,
                "Plugin-Version" to project.version,
                "Plugin-Description" to pluginDescription
            )
        )
    }
}
