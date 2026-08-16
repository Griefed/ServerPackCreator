import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
    alias(libs.plugins.i18n4k)
    kotlin("kapt")
}


// A consumable view of just this module's plugin jar, for the root build's copy tasks. Explicit
// rather than the legacy `archives` configuration, which Gradle 9 removes, and it carries the task
// dependency so the jar is built on demand.
val pluginArtifact: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(pluginArtifact.name, tasks.jar)
}

i18n4k {
    generationTargetPlatform = de.comahe.i18n4k.generator.GenerationTargetPlatform.JVM
}

/*
 CHANGE THESE VALUES
    FOR YOUR OWN
       ADDON

 Addon ID must be unique.
    Set it carefully!
 */
val pluginClass = "de.griefed.example.kotlin.Example"
val pluginId = "example-kotlin"
val pluginName = "Example Kotlin Plugin"
val pluginDescription = "An example plugin for ServerPackCreator, written in Kotlin, demonstrating all extension points available."
val pluginAuthor = "Griefed"

dependencies {
    annotationProcessor(libs.pf4j)
    kapt(libs.pf4j)
    /*
     * CAUTION: When copying the code of the example plugin, make sure to change the dependency on
     * the API to implementation("de.griefed:serverpackcreator:serverpackcreator-api:$VERSION")
     */
    implementation(project(":serverpackcreator-api"))

    // Testing
    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testImplementation(libs.mockk)
}

tasks.processResources {
    dependsOn(tasks.named("shipPluginDocuments"))
    // The expansion values are read into locals first: referencing the script's own properties from
    // inside the closure would capture the build script, which the configuration cache cannot
    // serialize.
    val expansions = mapOf(
        "version" to project.version,
        "plugin_id" to pluginId,
        "plugin_name" to pluginName,
        "plugin_description" to pluginDescription,
        "plugin_author" to pluginAuthor,
        "plugin_class" to pluginClass
    )
    filesMatching("plugin.toml") {
        expand(expansions)
    }
}

// The documents this plugin ships inside its own jar. Previously three bare `copy { }` calls inside
// the processResources CONFIGURATION block, so they ran whenever that task was configured — including
// on runs where processResources itself was UP-TO-DATE and did nothing — with no inputs, no outputs
// and no caching, writing into the source tree each time. Same fix as -api's shipRootDocuments.
//
// CHANGELOG.md is deliberately still listed even though this module has no such file at its root: the
// old copy silently did nothing for it, and `include` behaves the same way, so the shipped
// src/main/resources/CHANGELOG.md (which is tracked, and predates this) keeps whatever it holds. See
// the commit message — that stale file is worth a separate look, not a silent deletion here.
tasks.register<Copy>("shipPluginDocuments") {
    description = "Copies this plugin's own LICENSE, README and CHANGELOG into its resources."
    from(layout.projectDirectory) {
        include("LICENSE", "README.md", "CHANGELOG.md")
    }
    into(layout.projectDirectory.dir("src/main/resources"))
}

// Explicit dependency to remove Gradle 8 warning
tasks.processResources {
    dependsOn(tasks.generateI18n4kFiles)
}

tasks.test {
    dependsOn(":serverpackcreator-api:processTestResources")
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