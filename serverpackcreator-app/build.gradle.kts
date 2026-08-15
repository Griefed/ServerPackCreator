
plugins {
    id("serverpackcreator.dokka-conventions")
    id("org.springframework.boot") apply false
    id("serverpackcreator.application-conventions")
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
    // Clientside-mod verification engine (platforms, metadata/boot signals, list editing). The CLI
    // verbs in this module are thin wrappers over it; Playwright arrives transitively from here.
    api(project(":serverpackcreator-clientside"))
    api(libs.kotlinReflect)
    api(libs.commonsIo)
    api(libs.cronUtils)
    api(libs.jacksonModuleKotlin)
    compileOnly(libs.install4jRuntime)


    //CLI
    api(libs.picocli)

    //GUI
    api(libs.kotlinxCoroutinesSwing)
    api(libs.flatlaf)
    api(libs.flatlafExtras)
    api(libs.flatlafIntellijThemes)
    api(libs.flatlafFontsJetbrainsMono)
    api(libs.flatlafFontsInter)
    api(libs.flatlafFontsRoboto)
    api(libs.flatlafFontsRobotoMono)
    api(libs.miglayoutSwing)
    api(libs.svgSalamander)
    api(libs.balloontip)
    api(libs.tipoftheday)

    //WEB
    api(libs.springBootStarterWeb)
    api(libs.springBootStarterLog4j2)
    api(libs.springBootStarterDataMongodb)
    testImplementation(libs.springBootStarterTest) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)

    testImplementation(libs.springmockk)
    developmentOnly(libs.springBootDevtools)
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

tasks.bootJar {
    dependsOn(":serverpackcreator-api:processTestResources")
}

tasks.build {
    dependsOn(":generateLicenseReport")
    // This module bundles the license report and the built SPA, so both have to be finished first.
    // Declared here, by task PATH, rather than from the root reaching in with
    // `project("serverpackcreator-app").tasks.build.get()`: a string path is resolved lazily, whereas
    // reaching into another project's task container forces it to be evaluated, which is what used to
    // require evaluationDependsOnChildren() in the root build.
    mustRunAfter(":generateLicenseReport", ":serverpackcreator-web-frontend:build")
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
}
