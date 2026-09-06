import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
    kotlin("kapt")
}

// A consumable view of just this module's plugin jar, for the root build's copy task. Same shape as
// the example plugin's: explicit rather than the legacy `archives` configuration Gradle 9 removes,
// and it carries the task dependency so the jar is built on demand.
val pluginArtifact: Configuration = configurations.create("pluginArtifact") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(pluginArtifact.name, tasks.jar)
}

// The identity pf4j loads this plugin under. `pluginId` is the key its configuration is stored under
// (`<pluginId>.toml` in SPC's plugin-configs directory), so it must stay stable across releases —
// changing it orphans every user's saved selection.
val pluginClass = "de.griefed.serverpackcreator.plugin.grinder.GrinderPlugin"
val pluginId = "grinder"
val pluginName = "Grinder"
val pluginDescription =
    "Reads verdicts from a ServerPackCreator grinder and lets you pick which of them to exclude as clientside-only mods."
val pluginAuthor = "Griefed"

dependencies {
    annotationProcessor(libs.pf4j)
    kapt(libs.pf4j)
    // -api only, deliberately. A plugin compiles against the published API surface; depending on
    // -clientside or -grinder would tie this jar to modules that are not published and churn freely.
    // Everything this plugin needs from the grinder arrives over HTTP as JSON.
    implementation(project(":serverpackcreator-api"))

    // Testing
    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)
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
