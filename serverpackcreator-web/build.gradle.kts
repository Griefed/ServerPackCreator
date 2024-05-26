plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
    id("serverpackcreator.spring-conventions")
    id("org.springframework.boot") apply false
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

tasks.bootJar {
    dependsOn(":serverpackcreator-api:processTestResources")
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