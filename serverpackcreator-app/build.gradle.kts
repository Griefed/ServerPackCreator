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
    api("commons-io:commons-io:2.21.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    api("org.jetbrains.kotlin:kotlin-reflect:2.3.10")
    api("com.formdev:flatlaf:3.7.1")
    api("com.formdev:flatlaf-extras:3.7.1")
    api("com.formdev:flatlaf-intellij-themes:3.7.1")
    api("com.formdev:flatlaf-fonts-jetbrains-mono:2.304")
    api("com.formdev:flatlaf-fonts-inter:4.1")
    api("com.formdev:flatlaf-fonts-roboto:2.137")
    api("com.formdev:flatlaf-fonts-roboto-mono:3.000")
    api("com.miglayout:miglayout-swing:11.4.3")
    api("com.formdev:svgSalamander:1.1.4")
    api("net.java.balloontip:balloontip:1.2.4.1")
    api("com.cronutils:cron-utils:9.2.1")
    api("tokyo.northside:tipoftheday:0.6.0")
    compileOnly("com.install4j:install4j-runtime:12.0.3")

    //WEB
    api("org.jetbrains.kotlin:kotlin-reflect:2.3.10")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.1")
    api("org.springframework.boot:spring-boot-starter-web:4.0.3")
    api("org.springframework.boot:spring-boot-starter-log4j2:4.0.3")
    api("org.springframework.boot:spring-boot-starter-data-mongodb:4.0.3")
    testRuntimeOnly("com.h2database:h2:2.4.240")
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.3") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.2")

    testImplementation("com.ninja-squad:springmockk:5.0.1")
    developmentOnly("org.springframework.boot:spring-boot-devtools:4.0.3")
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
    useJUnitPlatform()
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