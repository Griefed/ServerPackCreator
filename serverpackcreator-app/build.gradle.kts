
plugins {
    id("serverpackcreator.dokka-conventions")
    id("org.springframework.boot") apply false
    id("serverpackcreator.application-conventions")
}

// Boot's BOM arrives as a Gradle `platform()` from `serverpackcreator.spring-conventions`; there is
// deliberately no `dependencyManagement { imports { mavenBom(...) } }` here and no
// `ext["<name>.version"]` overrides. See the comment in that convention plugin — the short version is
// that the BOM must constrain, not force, or it silently reverts this module's half of every version
// bump in `gradle/libs.versions.toml`.

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
    // springmockk pulls an older mockk transitively (1.14.6 against the catalog's 1.14.11). Declaring
    // the catalog's version explicitly out-ranks it, so this module tests against the same mockk as
    // -api instead of quietly running a different one. Without it the two drift on every mockk bump.
    testImplementation(libs.mockk)
    developmentOnly(libs.springBootDevtools)
    // Regenerates serverpackcreator-help/Writerside/api-docs.yaml from the live controllers:
    //   ./gradlew :serverpackcreator-app:bootRun --args="-web --home <dir>"
    //   curl localhost:8080/v3/api-docs.yaml > serverpackcreator-help/Writerside/api-docs.yaml
    // developmentOnly on purpose — swagger-ui has no business in the shipped jar. The 2.2.0 that
    // used to be commented here targets Spring Boot 3 and cannot resolve against Boot 4.
    developmentOnly(libs.springdocOpenapiStarterWebmvcUi)
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
    // Captured as a File so the action closes over that alone. Reading projectDir or calling
    // Project.mkdir inside a task action holds the project object, which the configuration cache
    // cannot serialize.
    val testHome = layout.projectDirectory.dir("tests").asFile
    doFirst {
        testHome.mkdirs()
        val gitkeep = File(testHome, ".gitkeep")
        if (!gitkeep.exists()) {
            gitkeep.writeText("Hi")
        }
    }
}
