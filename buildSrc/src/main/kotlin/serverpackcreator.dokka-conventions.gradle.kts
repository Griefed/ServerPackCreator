import de.griefed.common.gradle.constant.JDK_VERSION
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(buildDir.resolve("dokka"))
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
            //jdkVersion.set(JDK_VERSION.toInt())
            suppressGeneratedFiles.set(false)
            includes.from(projectDir.resolve("module.md"))
        }
    }
}