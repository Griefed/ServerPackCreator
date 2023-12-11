plugins {
    id("serverpackcreator.quasar-conventions")
}

tasks.register("copyDist", Copy::class) {
    from(projectDir.resolve("dist/spa"))
    into(rootDir.resolve("serverpackcreator-web/src/main/resources/static"))
}

tasks.build.get().finalizedBy(
        tasks.getByName("copyDist")
)