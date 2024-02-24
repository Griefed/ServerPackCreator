
import com.install4j.gradle.Install4jTask
import de.griefed.common.gradle.LicenseAgreementRenderer
import de.griefed.common.gradle.SubprojectLicenseFilter
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import java.time.LocalDate

plugins {
    idea
    kotlin("jvm")
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("com.github.jk1.dependency-license-report")
    id("com.install4j.gradle")
}

idea {
    project {
        languageLevel = IdeaLanguageLevel(properties["jdkVersion"])
        jdkName = properties["jdkVersion"] as String
        modules.forEach {
            it.isDownloadJavadoc = true
            it.isDownloadSources = true
            it.languageLevel = IdeaLanguageLevel(properties["jdkVersion"])
            it.jdkName = properties["jdkVersion"] as String
        }
    }
}

if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    //Ensure your install4j installation is available under this location
    setProperty("install4jHomeDir","C:\\Program Files\\install4j")
} else if (Os.isFamily(Os.FAMILY_UNIX))  {
    //Ensure your install4j installation is available under this location
    setProperty("install4jHomeDir","/opt/install4j")
} else if (Os.isFamily(Os.FAMILY_MAC)) {
    //Ensure your install4j installation is available under this location
    setProperty("install4jHomeDir","/Applications/install4j.app")
}

allprojects {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
evaluationDependsOnChildren()

project("serverpackcreator-web").tasks.build.get().mustRunAfter(
    project("serverpackcreator-web-frontend").tasks.build.get()
)

project("serverpackcreator-app").tasks.build.get().mustRunAfter(
    project("serverpackcreator-web").tasks.build.get()
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

    renderers = arrayOf(
        com.github.jk1.license.render.InventoryHtmlReportRenderer("index.html", "Dependency Licences"),
        com.github.jk1.license.render.InventoryMarkdownReportRenderer("licences.md", "Dependency Licenses"),
        LicenseAgreementRenderer("LICENSE-AGREEMENT"),
        LicenseAgreementRenderer("LICENSE-AGREEMENT.txt")
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
    delete(projectDir.resolve("serverpackcreator-gui/src/main/resources/de/griefed/resources/gui/LICENSE-AGREEMENT"))
}

tasks.register<Copy>("copyLicenseReport") {
    from(rootDir.resolve("licenses/LICENSE-AGREEMENT"))
    into(rootDir.resolve("serverpackcreator-gui/src/main/resources/de/griefed/resources/gui"))
}

tasks.generateLicenseReport {
    mustRunAfter(tasks.getByName("cleanLicenseReport"))
    finalizedBy(tasks.getByName("copyLicenseReport"))
}

install4j {
    installDir = file(properties["install4jHomeDir"].toString())
    verbose = true
}

task("media", Install4jTask::class) {
    mustRunAfter(tasks.build)
    release = version.toString()
    projectFile = "spc.install4j"
    variables = hashMapOf<Any, Any>(
        "projectDir" to rootDir.absolutePath,
        "projectVersion" to version.toString(),
        "projectYear" to LocalDate.now().year.toString()
    )
}