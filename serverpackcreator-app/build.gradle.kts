
import java.time.LocalDate
import java.util.*

plugins {
    id("serverpackcreator.dokka-conventions")
    id("org.springframework.boot") apply false
    id("serverpackcreator.application-conventions")
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

configurations {
    all {
        // Exclude logging from dependencies because we already have logging set up
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    api(project(":serverpackcreator-api"))

    //GUI
    api("commons-io:commons-io:2.16.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    api("org.jetbrains.kotlin:kotlin-reflect:1.9.23")
    api("com.formdev:flatlaf:3.4")
    api("com.formdev:flatlaf-extras:3.4")
    api("com.formdev:flatlaf-intellij-themes:3.4")
    api("com.formdev:flatlaf-fonts-jetbrains-mono:2.304")
    api("com.formdev:flatlaf-fonts-inter:4.0")
    api("com.formdev:flatlaf-fonts-roboto:2.137")
    api("com.formdev:flatlaf-fonts-roboto-mono:3.000")
    api("com.miglayout:miglayout-swing:11.3")
    api("com.formdev:svgSalamander:1.1.4")
    api("net.java.balloontip:balloontip:1.2.4.1")
    api("com.cronutils:cron-utils:9.2.1")
    api("tokyo.northside:tipoftheday:0.4.2")

    //WEB
    api("org.jetbrains.kotlin:kotlin-reflect:1.9.23")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    api("org.springframework.boot:spring-boot-starter-web:3.2.4")
    api("org.springframework.boot:spring-boot-starter-log4j2:3.2.4")
    api("org.springframework.boot:spring-boot-starter-data-jpa:3.2.5")
    api("org.postgresql:postgresql:42.7.3")
    api("org.javassist:javassist:3.30.2-GA")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("com.h2database:h2:2.2.224")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.4")
    developmentOnly("org.springframework.boot:spring-boot-devtools:3.2.5")
    //developmentOnly("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
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

tasks.bootJar {
    dependsOn(":serverpackcreator-api:processTestResources")
}

task("copyDependencies", Copy::class) {
    dependsOn(":serverpackcreator-api:processTestResources")
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

tasks.test {
    dependsOn(":serverpackcreator-api:processTestResources")
    systemProperty("java.util.logging.manager","org.jboss.logmanager.LogManager")
    doFirst {
        val tests = File(projectDir,"tests").absoluteFile
        mkdir(tests.absolutePath)
        val gitkeep = File(tests,".gitkeep").absoluteFile
        if (!gitkeep.exists()) {
            File(tests,".gitkeep").writeText("Hi")
        }
    }
}

// https://docs.oracle.com/en/java/javase/14/docs/specs/man/jpackage.html
// https://github.com/petr-panteleyev/jpackage-gradle-plugin
tasks.jpackage {
    val packagerResources: File = File(projectDir.absoluteFile, "jpackagerResources").absoluteFile
    val parent: File = projectDir.parentFile.absoluteFile
    val preReleaseRegex: Regex = "(\\d+\\.\\d+\\.\\d+)-(alpha|beta).\\d+".toRegex()
    val ver: String = project.version.toString()
    dependsOn("build", "copyDependencies", "copyJar", "cleanTmpPackager")
    aboutUrl = "https://serverpackcreator.de"
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