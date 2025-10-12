import java.util.prefs.Preferences

plugins {
    id("serverpackcreator.dokka-conventions")
    id("org.springframework.boot") apply false
    id("serverpackcreator.application-conventions")
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://maven.ej-technologies.com/repository") }
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

    //CLI
    api("info.picocli:picocli-shell-jline3:4.7.7")

    //GUI
    api("commons-io:commons-io:2.20.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    api("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
    api("com.formdev:flatlaf:3.5.1")
    api("com.formdev:flatlaf-extras:3.6")
    api("com.formdev:flatlaf-intellij-themes:3.5.4")
    api("com.formdev:flatlaf-fonts-jetbrains-mono:2.304")
    api("com.formdev:flatlaf-fonts-inter:4.1")
    api("com.formdev:flatlaf-fonts-roboto:2.137")
    api("com.formdev:flatlaf-fonts-roboto-mono:3.000")
    api("com.miglayout:miglayout-swing:11.4.2")
    api("com.formdev:svgSalamander:1.1.4")
    api("net.java.balloontip:balloontip:1.2.4.1")
    api("com.cronutils:cron-utils:9.2.1")
    api("tokyo.northside:tipoftheday:0.4.2")
    compileOnly("com.install4j:install4j-runtime:11.0.4")

    //WEB
    api("org.jetbrains.kotlin:kotlin-reflect:2.1.21")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
    api("org.springframework.boot:spring-boot-starter-web:3.5.6")
    api("org.springframework.boot:spring-boot-starter-log4j2:3.5.6")
    api("org.springframework.boot:spring-boot-starter-data-mongodb:3.5.6")
    testRuntimeOnly("com.h2database:h2:2.3.232")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.6") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.2")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    developmentOnly("org.springframework.boot:spring-boot-devtools:3.5.6")
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

tasks.processResources {
    dependsOn(":copyLicenseReport")
}

tasks.sourcesJar {
    dependsOn(":copyLicenseReport")
}

tasks.bootJar {
    dependsOn(":serverpackcreator-api:processTestResources")
}

tasks.build {
    dependsOn(":generateLicenseReport")
    doLast {
        tasks.dokkaJavadocJar
    }
    finalizedBy(tasks.dokkaJavadocJar)
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
    Preferences.userRoot().node("ServerPackCreator").clear()
    Preferences.userRoot().node("ServerPackCreator").sync()
}

tasks.signMavenJavaPublication {
    dependsOn(tasks.dokkaJavadocJar)
}