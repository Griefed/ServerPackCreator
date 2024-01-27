repositories {
    mavenCentral()
}

plugins {
    id("serverpackcreator.java-conventions")

    // Allows to package executable jar or war archives, run Spring Boot applications, and use the dependency management
    // https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
    id("org.springframework.boot")
    id("io.spring.dependency-management")

    // Classes annotated with @Configuration, @Controller, @RestController, @Service or @Repository are automatically opened
    // https://kotlinlang.org/docs/reference/compiler-plugins.html#spring-support
    kotlin("plugin.spring")
    kotlin("plugin.allopen")
    kotlin("plugin.jpa")
}

apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.allopen")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

springBoot {
    // Creates META-INF/build-info.properties for Spring Boot Actuator
    buildInfo()
}
