import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("serverpackcreator.java-conventions")

    // Allows to package executable jar or war archives and run Spring Boot applications.
    // https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
    //
    // `io.spring.dependency-management` is deliberately NOT applied. That plugin turns Boot's BOM into
    // *forced* versions that beat every transitive request, and Boot's BOM manages far more than Spring
    // — kotlin, kotlin-coroutines, kotlin-serialization, jackson, log4j2, junit-jupiter, mongodb. With it
    // applied, bumping any of those in `gradle/libs.versions.toml` upgraded every other module and was
    // silently reverted here, which is not a warning or a build failure but a NoSuchMethodError the first
    // time the newer API is actually called. It cost 16 app tests on the coroutines 1.11.0 bump. The BOM
    // is imported below as a Gradle `platform()` instead, where its versions are ordinary constraints
    // that lose to a higher request — so the catalog wins and Boot still versions everything we do not
    // pin ourselves. Do not re-apply the plugin to "tidy" the platform away.
    id("org.springframework.boot")

    // Classes annotated with @Configuration, @Controller, @RestController, @Service or @Repository are automatically opened
    // https://kotlinlang.org/docs/reference/compiler-plugins.html#spring-support
    kotlin("plugin.spring")
    kotlin("plugin.allopen")
    kotlin("plugin.jpa")
}

// The catalog is not available as the `libs` accessor inside a precompiled script plugin, so the
// alias is looked up explicitly — but it IS the catalog's `springBootDependencies` entry, not a
// coordinate string rebuilt from a version. Keeps `gradle/libs.versions.toml` the single source of
// truth for the BOM as well as for the starters: before this, the BOM came from
// `SpringBootPlugin.BOM_COORDINATES`, i.e. the *Gradle plugin's* version (`springGradle`), so a
// `springBoot` bump left the BOM behind and `spring-boot` itself resolved 4.0.2 while
// `spring-boot-starter-web` resolved 4.1.0.
val springBootBom = extensions.getByType<VersionCatalogsExtension>()
    .named("libs").findLibrary("springBootDependencies").get()

dependencies {
    // Applies to compileClasspath/runtimeClasspath/testRuntimeClasspath, which all extend
    // `implementation`. `developmentOnly` extends nothing, so it needs the platform of its own —
    // without it the versionless devtools dependency below has no version to resolve.
    implementation(platform(springBootBom))
    developmentOnly(platform(springBootBom))

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
