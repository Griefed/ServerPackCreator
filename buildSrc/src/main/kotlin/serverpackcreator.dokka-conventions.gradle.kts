import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(layout.buildDirectory.asFile.get().resolve("dokka"))
    dokkaSourceSets {
        configureEach {
            documentedVisibilities.set(
                setOf(
                    DokkaConfiguration.Visibility.PUBLIC,
                    DokkaConfiguration.Visibility.PROTECTED,
                    DokkaConfiguration.Visibility.PACKAGE,
                )
            )
            skipDeprecated.set(false)
            reportUndocumented.set(true)
            skipEmptyPackages.set(true)
            jdkVersion.set(21)
            suppressGeneratedFiles.set(false)
            includes.from(
                projectDir.resolve("module.md")
            )
        }
    }
}

val dokkaHtml by tasks.getting(DokkaTask::class)

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaHtml, tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    from(dokkaHtml.outputDirectory)
}