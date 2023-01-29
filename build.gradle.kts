plugins {
    idea
    kotlin("jvm")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.github.jk1.dependency-license-report")
}

idea {
    module.isDownloadJavadoc = true
    module.isDownloadSources = true
}

allprojects {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
evaluationDependsOnChildren()

val serverpackcreatorApp = project("serverpackcreator-app")
val serverpackcreatorWebFrontend = project("serverpackcreator-web-frontend")

serverpackcreatorApp.tasks.getByName("build").mustRunAfter(
    serverpackcreatorWebFrontend.tasks.getByName("build")
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

    projects = arrayOf(
        project(":serverpackcreator-api"),
        project(":serverpackcreator-app"),
        project(":serverpackcreator-cli"),
        project(":serverpackcreator-gui"),
        project(":serverpackcreator-updater"),
        project(":serverpackcreator-web")
    )

    configurations = arrayOf("runtimeClasspath", "compileClasspath")


    filters = arrayOf(
        com.github.jk1.license.filter.LicenseBundleNormalizer()
    )

    renderers = arrayOf<com.github.jk1.license.render.ReportRenderer>(
        com.github.jk1.license.render.InventoryHtmlReportRenderer("index.html", "Dependency Licences"),
        com.github.jk1.license.render.InventoryMarkdownReportRenderer("licences.md", "Dependency Licenses")
    )
}

val appPlugins = File("serverpackcreator-app/tests/plugins")
val apiPlugins = File("serverpackcreator-api/src/jvmTest/resources/testresources/plugins")
val kotlinPlugin =
    project.childProjects["serverpackcreator-plugin-example"]?.tasks?.jar?.get()?.archiveFile?.get()?.asFile?.toPath()

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