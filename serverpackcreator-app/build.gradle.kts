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
    api("de.griefed:versionchecker:1.1.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
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

task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath).into("$buildDir/jars")
}

task("copyJar", Copy::class) {
    from(tasks.jar).into("$buildDir/jars")
}

tasks.register<Delete>("cleanTmpPackager") {
    delete("$buildDir/tmp/jpackager")
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
    appDescription = "Create server packs from Minecraft Forge, Fabric, Quilt or LegacyFabric modpacks."
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
    destination = "$buildDir/dist"
    icon = File(packagerResources, "app.png").path
    input = "$buildDir/jars"
    javaOptions = listOf("-Dfile.encoding=UTF-8", "-Dlog4j2.formatMsgNoLookups=true")
    licenseFile = parent.path + "/LICENSE"
    mainJar = tasks.jar.get().archiveFileName.get()
    mainClass = "de.griefed.serverpackcreator.app.ServerPackCreatorKt"
    resourceDir = packagerResources.path
    runtimeImage = System.getProperty("java.home")
    temp = "$buildDir/tmp/jpackager"
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
        winConsole = true
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
