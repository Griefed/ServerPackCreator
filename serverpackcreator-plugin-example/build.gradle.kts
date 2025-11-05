import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("serverpackcreator.kotlin-conventions")
    id("de.comahe.i18n4k") version "0.11.1"
    kotlin("kapt")
}

repositories {
    mavenCentral()
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
    annotationProcessor("org.pf4j:pf4j:3.13.0")
    kapt("org.pf4j:pf4j:3.13.0")
    /*
     * CAUTION: When copying the code of the example plugin, make sure to change the dependency on
     * the API to implementation("de.griefed:serverpackcreator:serverpackcreator-api:$VERSION")
     */
    implementation(project(":serverpackcreator-api"))


    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.0")
}

tasks.processResources {
    filesMatching("plugin.toml") {
        expand(
            "version" to project.version,
            "plugin_id" to pluginId,
            "plugin_name" to pluginName,
            "plugin_description" to pluginDescription,
            "plugin_author" to pluginAuthor,
            "plugin_class" to pluginClass
        )
    }
    copy {
        from(layout.projectDirectory.file("LICENSE"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(layout.projectDirectory.file("README.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(layout.projectDirectory.file("CHANGELOG.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
}

// Explicit dependency to remove Gradle 8 warning
tasks.processResources {
    dependsOn(tasks.generateI18n4kFiles)
}

// Explicit dependency to remove Gradle 8 warning
tasks.sourcesJar {
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