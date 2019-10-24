import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
}

group = "app.fiber"
version = "1.0"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(Dependencies.stdlib)

    implementation(Dependencies.ktorCore)
    implementation(Dependencies.ktorNetty)

    implementation(Dependencies.logback)
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    val exportLib = register("exportDependencies", Copy::class) {
        configurations.implementation.get().isCanBeResolved = true

        into(buildDir.path + "/export")
        from(configurations.implementation.get())

        this.group = "app.fiber"
    }

    build {
        dependsOn(exportLib)
    }
    
    jar {
        archiveVersion.set("")
    }

}

object Version {

    const val kotlin = "1.3.50"
    const val ktor = "1.2.5"
    const val logback = "1.2.3"

}

object Dependencies {

    const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlin}"

    const val ktorCore = "io.ktor:ktor-server-core:${Version.ktor}"
    const val ktorNetty = "io.ktor:ktor-server-netty:${Version.ktor}"

    const val logback = "ch.qos.logback:logback-classic:${Version.logback}"
}