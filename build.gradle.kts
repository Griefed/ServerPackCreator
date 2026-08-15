import com.install4j.gradle.Install4jTask
import de.griefed.common.gradle.LicenseAgreementRenderer
import de.griefed.common.gradle.SubprojectLicenseFilter
import org.gradle.internal.os.OperatingSystem
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import java.time.LocalDate

plugins {
    idea
    kotlin("jvm")
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.github.jk1.dependency-license-report")
    id("com.install4j.gradle")
}

idea {
    project {
        languageLevel = IdeaLanguageLevel(21)
        modules.forEach {
            it.isDownloadJavadoc = true
            it.isDownloadSources = true
            it.languageLevel = IdeaLanguageLevel(21)
        }
    }
}


nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}


licenseReport {
    outputDir = "$projectDir/licenses"
    configurations = arrayOf("runtimeClasspath", "compileClasspath")

    filters = arrayOf(
        com.github.jk1.license.filter.LicenseBundleNormalizer(),
        SubprojectLicenseFilter()
    )

    renderers = arrayOf(
        com.github.jk1.license.render.InventoryHtmlReportRenderer("index.html", "Dependency Licences"),
        com.github.jk1.license.render.InventoryMarkdownReportRenderer("licences.md", "Dependency Licenses"),
        LicenseAgreementRenderer("LICENSE-AGREEMENT"),
        LicenseAgreementRenderer("LICENSE-AGREEMENT.txt")
    )
}

// The example plugin's jar, consumed as an ARTIFACT rather than by reaching into the other project's
// task container. `project.childProjects[...]?.tasks?.jar?.get()?.archiveFile?.get()` needed that
// project to be evaluated already — which is what evaluationDependsOnChildren() was there for — and
// still ended in a `!!` at both use sites because every link in the chain is nullable. A dependency on
// the project resolves lazily and carries the task dependency with it, so the jar is built on demand.
val examplePlugin: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    examplePlugin(project(path = ":serverpackcreator-plugin-example", configuration = "pluginArtifact"))
}

val appPlugins = layout.projectDirectory.dir("serverpackcreator-app/tests/plugins")
val apiPlugins = layout.projectDirectory.dir("serverpackcreator-api/src/test/resources/testresources/plugins")

tasks.register<Delete>("cleanAppPlugins") {
    delete(fileTree(appPlugins) { include("**/*.jar") })
}

tasks.register<Copy>("copyExamplePluginsToApp") {
    description = "Refreshes the example plugin in the app's manual-test plugins directory."
    dependsOn("cleanAppPlugins")
    from(examplePlugin)
    into(appPlugins)
}

tasks.register<Delete>("cleanApiUnitTestPlugins") {
    delete(fileTree(apiPlugins) { include("**/*.jar") })
}

tasks.register<Copy>("copyPluginsApiUnitTests") {
    description = "Refreshes the example plugin ApiPluginsTest loads through pf4j."
    dependsOn("cleanApiUnitTestPlugins")
    from(examplePlugin)
    into(apiPlugins)
}

tasks.register<Delete>("cleanLicenseReport") {
    delete(projectDir.resolve("serverpackcreator-app/src/main/resources/de/griefed/resources/gui/LICENSE-AGREEMENT"))
}

tasks.register<Copy>("copyLicenseReport") {
    from(rootDir.resolve("licenses/LICENSE-AGREEMENT"))
    into(rootDir.resolve("serverpackcreator-app/src/main/resources/de/griefed/resources/gui"))
}

tasks.generateLicenseReport {
    mustRunAfter(tasks.getByName("cleanLicenseReport"))
    finalizedBy(tasks.getByName("copyLicenseReport"))
}

install4j {
    //Set the install4jHomeDir-property for building on your own machine, or use the paths listed below according
    //to your operating system family.
    installDir = if (properties["install4jHomeDir"].toString().isNotBlank()) {
        file(properties["install4jHomeDir"].toString())
    } else if (OperatingSystem.current().isWindows) {
        file("C:\\Program Files\\install4j")
    } else if (OperatingSystem.current().isMacOsX) {
        //Ensure your install4j installation is available under this location
        file("/Applications/install4j.app")
    } else {
        //Ensure your install4j installation is available under this location
        file("/opt/install4j")
    }
    verbose = true
}

tasks.register<Install4jTask>("media") {
    dependsOn(tasks.build)
    verbose = true
    release = version.toString()
    projectFile = file("spc.install4j")
    variables.putAll(
        mutableMapOf(
        "projectDir" to rootDir.absolutePath,
        "projectVersion" to version.toString(),
        "projectYear" to LocalDate.now().year.toString()
    ))
}