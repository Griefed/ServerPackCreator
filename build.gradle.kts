import com.install4j.gradle.Install4jTask
import de.griefed.common.gradle.LicenseAgreementRenderer
import de.griefed.common.gradle.SubprojectLicenseFilter
import org.gradle.internal.os.OperatingSystem
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import java.time.LocalDate

plugins {
    idea
    kotlin("jvm")
    alias(libs.plugins.nexusPublish)
    id("com.github.jk1.dependency-license-report")
    alias(libs.plugins.install4j)
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

    // springdoc exists only to regenerate serverpackcreator-help/Writerside/api-docs.yaml and is declared
    // `developmentOnly`, so it is NOT in the shipped jar -- verified by listing the bootJar, which contains
    // no springdoc or swagger entry. It nonetheless reaches compileClasspath/runtimeClasspath above, so
    // without this the LICENSE-AGREEMENT files -- documents about what ships -- would list a build tool,
    // and churn 303 lines in two shipped files on every springdoc bump.
    excludeGroups = arrayOf("org.springdoc")

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
// `create(name) { }` rather than the `creating` delegate, deprecated in Gradle 9.7 (9.6 upgrading
// guide). The delegate only supplied the name from the property, so the name is unchanged — which
// matters, because `pluginArtifact` below is consumed by string name.
val examplePlugin: Configuration = configurations.create("examplePlugin") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

// The grinder plugin's jar, kept in its own configuration rather than added to `examplePlugin`:
// only the example may reach the api test-resources directory below, and one configuration per
// destination is what keeps that separation from depending on somebody remembering it.
val grinderPlugin: Configuration = configurations.create("grinderPlugin") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    examplePlugin(project(path = ":serverpackcreator-plugin-example", configuration = "pluginArtifact"))
    grinderPlugin(project(path = ":serverpackcreator-plugin-grinder", configuration = "pluginArtifact"))
}

val appPlugins = layout.projectDirectory.dir("serverpackcreator-app/tests/plugins")
val apiPlugins = layout.projectDirectory.dir("serverpackcreator-api/src/test/resources/testresources/plugins")

tasks.register<Delete>("cleanAppPlugins") {
    delete(fileTree(appPlugins) { include("**/*.jar") })
}

tasks.register<Copy>("copyPluginsToApp") {
    description = "Refreshes the example and grinder plugins in the app's manual-test plugins directory."
    dependsOn("cleanAppPlugins")
    from(examplePlugin)
    from(grinderPlugin)
    into(appPlugins)
}

tasks.register<Delete>("cleanApiUnitTestPlugins") {
    delete(fileTree(apiPlugins) { include("**/*.jar") })
}

// DELIBERATELY the example plugin alone. ApiPluginsTest loops over every plugin jar it finds here and
// asserts each one provides ALL SIX extension types; the grinder plugin provides two (TabExtension and
// PreGenExtension), so adding it to this copy turns that suite red. The example is the only plugin that
// exercises every extension point, which is exactly why it is the one this test loads.
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
    mustRunAfter(tasks.named("cleanLicenseReport"))
    finalizedBy(tasks.named("copyLicenseReport"))
}

install4j {
    //Set the install4jHomeDir-property for building on your own machine, or use the paths listed below according
    //to your operating system family.
    // `providers.gradleProperty` instead of the `properties` map, which Gradle 9.7 deprecates. It also
    // removes a trap: `properties["x"]` on an ABSENT key returns null, whose `.toString()` is the string
    // "null" — which is not blank, so the old guard passed and installDir became a directory named
    // `null`. That never fired only because gradle.properties declares `install4jHomeDir=` empty, making
    // that empty declaration load-bearing. It no longer is.
    installDir = providers.gradleProperty("install4jHomeDir").orNull
        ?.takeIf { it.isNotBlank() }
        ?.let { file(it) }
        ?: when {
            OperatingSystem.current().isWindows -> file("C:\\Program Files\\install4j")
            //Ensure your install4j installation is available under this location
            OperatingSystem.current().isMacOsX -> file("/Applications/install4j.app")
            //Ensure your install4j installation is available under this location
            else -> file("/opt/install4j")
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