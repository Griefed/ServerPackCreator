import com.install4j.gradle.Install4jTask
import de.griefed.common.gradle.LicenseAgreementRenderer
import de.griefed.common.gradle.SubprojectLicenseFilter
//import org.cyclonedx.model.AttachmentText
//import org.cyclonedx.model.License
//import org.cyclonedx.model.OrganizationalContact
import org.gradle.internal.os.OperatingSystem
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import java.time.LocalDate

plugins {
    idea
    kotlin("jvm")
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.github.jk1.dependency-license-report")
    id("com.install4j.gradle")
    //id("org.cyclonedx.bom") version "1.10.0"
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

allprojects {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    tasks.withType<Test> {
        jvmArgs("-XX:+EnableDynamicAgentLoading", "-Djdk.attach.allowAttachSelf=true")
    }
}
evaluationDependsOnChildren()

project("serverpackcreator-app").tasks.build.get().mustRunAfter(
    tasks.getByName("generateLicenseReport"),
    project("serverpackcreator-web-frontend").tasks.build.get()
)

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

/*
tasks.cyclonedxBom {
    setIncludeConfigs(listOf("runtimeClasspath"))
    setSkipConfigs(listOf("compileClasspath", "testCompileClasspath"))
    setProjectType("application")
    setSchemaVersion("1.5")
    setDestination(project.file("build/reports"))
    setOutputName("bom")
    //setOutputFormat("json")
    setIncludeBomSerialNumber(true)

    val organizationalContact = OrganizationalContact()
    organizationalContact.name = "Griefed"
    organizationalContact.email = "griefed@griefed.de"
    setOrganizationalEntity { oe ->
        oe.name = "Griefed"
        oe.urls = listOf("griefed.de")
        oe.addContact(organizationalContact)
    }

    val attachementText = AttachmentText()
    attachementText.text = File(projectDir,"LICENSE").readText()
    val license = License()
    license.name = "LGPL-2.1"
    license.setLicenseText(attachementText)
    license.url = "https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE"
    setLicenseChoice { lc ->
        lc.addLicense(license)
    }
}
*/

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
val apiPlugins = File("serverpackcreator-api/src/test/resources/testresources/plugins")
val kotlinPlugin = project.childProjects["serverpackcreator-plugin-example"]?.tasks?.jar?.get()?.archiveFile?.get()?.asFile?.toPath()
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
    } else if (OperatingSystem.current().isLinux)  {
        //Ensure your install4j installation is available under this location
        file("/opt/install4j")
    } else {
        file(properties["install4jHomeDir"].toString())
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