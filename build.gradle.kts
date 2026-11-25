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
    implementation(libs.caffeine)

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
    relocate("com.github.benmanes.caffeine", "sh.miles.powerai.libs.caffeine")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

runPaper.disablePluginJarDetection()
val hotswapJar by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    group = "run paper"
    description = "Builds an unrelocated shaded jar for runServer so hotswapped classes still link"
    archiveClassifier = "hotswap"
    from(sourceSets.main.map { it.output })
    configurations.add(project.configurations.runtimeClasspath)
}

tasks.runServer {
    minecraftVersion(libs.versions.minecraft.get())
    pluginJars(hotswapJar.flatMap { it.archiveFile })
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(25)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}
