import java.time.LocalDate
import java.util.*

plugins {
    id("serverpackcreator.application-conventions")
}

repositories {
    mavenCentral()
}

configurations {
    all {
        // Exclude logging from dependencies because we already have logging set up
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
}

dependencies {
    api(project(":serverpackcreator-cli"))
    api(project(":serverpackcreator-gui"))
    api(project(":serverpackcreator-web"))
    api(project(":serverpackcreator-updater"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

springBoot {
    mainClass.set("de.griefed.serverpackcreator.app.ServerPackCreatorKt")
}

tasks.clean {
    doFirst {
        delete {
            fileTree("tests") {
                exclude(".gitkeep")
            }
        }
    }
}

tasks.processResources {
    copy {
        from(rootProject.layout.projectDirectory.file("CHANGELOG.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("CODE_OF_CONDUCT.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("CONTRIBUTING.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("HELP.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("LICENSE"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("README.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
}

task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath).into("${layout.buildDirectory.asFile.get()}/jars")
}

task("copyJar", Copy::class) {
    dependsOn(tasks.sourcesJar, tasks.bootJar, tasks.javadocJar, tasks.jar)
    from("${layout.buildDirectory.asFile.get()}/libs") {
        include("*.jar")
    }.into("${layout.buildDirectory.asFile.get()}/jars")
}

tasks.register<Delete>("cleanTmpPackager") {
    delete("${layout.buildDirectory.asFile.get()}/tmp/jpackager")
}

// https://docs.oracle.com/en/java/javase/14/docs/specs/man/jpackage.html
// https://github.com/petr-panteleyev/jpackage-gradle-plugin
tasks.jpackage {
    val packagerResources: File = File(projectDir.absoluteFile, "jpackagerResources").absoluteFile
    val parent: File = projectDir.parentFile.absoluteFile
    val preReleaseRegex: Regex = "(\\d+\\.\\d+\\.\\d+)-(alpha|beta).\\d+".toRegex()
    val ver: String = project.version.toString()
    dependsOn("build", "copyDependencies", "copyJar", "cleanTmpPackager")
    aboutUrl = "https://www.griefed.de/#/serverpackcreator"
    appDescription = "Create server packs from Minecraft Forge, NeoForge, Fabric, Quilt or LegacyFabric modpacks."
    appName = "ServerPackCreator"
    appVersion = if (ver == "dev") {
        val current = LocalDate.now().toString().split("-")
        "${current[0].substring(2)}.${current[1]}.${current[2]}"
    } else if (ver.matches(preReleaseRegex)) {
        ver.replace(preReleaseRegex, "$1")
    } else {
        ver
    }
    copyright = "Copyright (C) ${Calendar.getInstance().get(Calendar.YEAR)} Griefed"
    destination = "${layout.buildDirectory.asFile.get()}/dist"
    icon = File(packagerResources, "app.png").path
    input = "${layout.buildDirectory.asFile.get()}/jars"
    javaOptions = listOf("-Dfile.encoding=UTF-8", "-Dlog4j2.formatMsgNoLookups=true")
    licenseFile = parent.path + "/licenses/LICENSE-AGREEMENT"
    mainJar = tasks.jar.get().archiveFileName.get()
    mainClass = "de.griefed.serverpackcreator.app.ServerPackCreatorKt"
    resourceDir = packagerResources.path
    runtimeImage = System.getProperty("java.home")
    temp = "${layout.buildDirectory.asFile.get()}/tmp/jpackager"
    vendor = "griefed.de"
    verbose = true
    mac {
        icon = File(packagerResources, "app.icns").path
        type = org.panteleyev.jpackage.ImageType.PKG
        macAppCategory = "utilities"
        macPackageIdentifier = "ServerPackCreator"
        macPackageName = "ServerPackCreator"
    }
    windows {
        icon = File(packagerResources, "app.ico").path
        type = org.panteleyev.jpackage.ImageType.MSI
        winConsole = false
        winMenu = true
        winMenuGroup = "ServerPackCreator"
        winPerUserInstall = false
        winShortcut = true
        winShortcutPrompt = true
    }
    linux {
        icon = File(packagerResources, "app.png").path
        type = org.panteleyev.jpackage.ImageType.DEB
        linuxAppCategory = "utils"
        linuxAppRelease = appVersion
        linuxDebMaintainer = "griefed@griefed.de"
        linuxMenuGroup = "Utility;FileTools;Java;"
        linuxRpmLicenseType = "LGPL-2.1"
        linuxShortcut = true
    }
}
