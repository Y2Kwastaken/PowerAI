plugins {
    java
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

group = rootProject.group
version = rootProject.version

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://maven.squiddev.cc") {
        name = "squiddev"
        content { includeGroup("cc.tweaked") }
    }
}

dependencies {
    compileOnly(libs.paper.api)

    implementation(libs.cobalt)

    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "apiVersion" to libs.versions.minecraft.get(),
    )
    inputs.properties(props)
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveClassifier = "dev"
}

tasks.shadowJar {
    archiveClassifier = ""
    relocate("org.squiddev.cobalt", "sh.miles.powerai.libs.cobalt")
    relocate("cc.tweaked.cobalt", "sh.miles.powerai.libs.cobalt.tweaked")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

tasks.runServer {
    minecraftVersion(libs.versions.minecraft.get())
}
