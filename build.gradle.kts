import de.griefed.common.gradle.SubprojectLicenseFilter
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
    idea
    kotlin("jvm")
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("com.github.jk1.dependency-license-report")
}

idea {
    project {
        languageLevel = IdeaLanguageLevel("17")
        jdkName = "17"
        modules.forEach {
            it.isDownloadJavadoc = true
            it.isDownloadSources = true
            it.languageLevel = IdeaLanguageLevel("17")
            it.jdkName = "17"
        }
    }
}

allprojects {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
evaluationDependsOnChildren()

project("serverpackcreator-app").tasks.getByName("build").mustRunAfter(
    project("serverpackcreator-web-frontend").tasks.getByName("build")
)

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
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

    renderers = arrayOf<com.github.jk1.license.render.ReportRenderer>(
        com.github.jk1.license.render.InventoryHtmlReportRenderer("index.html", "Dependency Licences"),
        com.github.jk1.license.render.InventoryMarkdownReportRenderer("licences.md", "Dependency Licenses"),
        com.github.jk1.license.render.TextReportRenderer()
    )
}

val appPlugins = File("serverpackcreator-app/tests/plugins")
val apiPlugins = File("serverpackcreator-api/src/jvmTest/resources/testresources/plugins")
val kotlinPlugin =
    project.childProjects["serverpackcreator-plugin-example"]?.tasks?.jar?.get()?.archiveFile?.get()?.asFile?.toPath()
val licenseReports = File("licenses")

tasks.register<Delete>("cleanAppPlugins") {
    delete(
        fileTree(appPlugins) {
            include("**/*.jar")
        }
    )
}

tasks.register<Copy>("copyExamplePluginsToApp") {
    dependsOn(
        "cleanAppPlugins",
        ":serverpackcreator-plugin-example:build"
    )
    appPlugins.mkdirs()
    from(kotlinPlugin!!)
    into(appPlugins)
}

tasks.register<Delete>("cleanApiUnitTestPlugins") {
    delete(
        fileTree(apiPlugins) {
            include("**/*.jar")
        }
    )
}

tasks.register<Copy>("copyPluginsApiUnitTests") {
    dependsOn(
        "cleanApiUnitTestPlugins",
        ":serverpackcreator-plugin-example:build"
    )
    from(kotlinPlugin!!)
    into(apiPlugins)
}

tasks.register<Delete>("cleanLicenseReport") {
    delete(licenseReports)
    delete(projectDir.resolve("serverpackcreator-gui/src/main/resources/de/griefed/resources/gui/THIRD-PARTY-NOTICES.txt"))
}

tasks.register<Copy>("copyLicenseReport") {
    from(rootDir.resolve("licenses/THIRD-PARTY-NOTICES.txt"))
    into(rootDir.resolve("serverpackcreator-gui/src/main/resources/de/griefed/resources/gui"))
}

tasks.generateLicenseReport {
    mustRunAfter(tasks.getByName("cleanLicenseReport"))
    finalizedBy(tasks.getByName("copyLicenseReport"))
}