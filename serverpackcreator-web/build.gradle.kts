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
    api("org.jetbrains.kotlin:kotlin-reflect:1.8.20")
    api("org.apache.activemq:artemis-jms-server:2.28.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    api("org.springframework.boot:spring-boot-starter-web:2.7.7")
    api("org.springframework.boot:spring-boot-starter-log4j2:2.7.7")
    api("org.springframework.boot:spring-boot-starter-data-jpa:2.7.7")
    api("org.springframework.boot:spring-boot-starter-artemis:2.7.7")
    api("com.github.gwenn:sqlite-dialect:0.1.2")
    api("org.xerial:sqlite-jdbc:3.40.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.7")
    developmentOnly("org.springframework.boot:spring-boot-devtools:2.7.7")
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