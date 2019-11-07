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

    implementation(Dependencies.cassandra)
    implementation(Dependencies.cassandraQueryBuilder)

    implementation(Dependencies.koin)

    implementation(Dependencies.ktorCore)
    implementation(Dependencies.ktorNetty)
    implementation(Dependencies.ktorJwt)
    implementation(Dependencies.ktorGson)

    implementation(Dependencies.ktorClient)

    implementation(Dependencies.logback)
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    val checkLib = register("checkExportedDependencies") {
        this.group = "fiber"
        val path = "${buildDir.path}/export"

        file(path).listFiles()
            ?.filter { !configurations.runtimeClasspath.get().map { file -> file.name }.contains(it.name) }
            ?.forEach { it.delete() }
    }

    val exportLib = register("exportDependencies", Copy::class) {
        this.group = "fiber"

        into("${buildDir.path}/export")
        from(configurations.runtimeClasspath.get())

        dependsOn(checkLib)
    }

    build {
        dependsOn(exportLib)
    }
    
    jar {
        archiveVersion.set("")
    }

}

object Version {
    const val cassandra = "4.3.0"
    const val koin = "2.0.1"
    const val kotlin = "1.3.50"
    const val ktor = "1.2.5"
    const val logback = "1.2.3"
}

object Dependencies {
    const val cassandra = "com.datastax.oss:java-driver-core:${Version.cassandra}"
    const val cassandraQueryBuilder = "com.datastax.oss:java-driver-query-builder:${Version.cassandra}"

    const val koin = "org.koin:koin-ktor:${Version.koin}"

    const val ktorCore = "io.ktor:ktor-server-core:${Version.ktor}"

    const val ktorNetty = "io.ktor:ktor-server-netty:${Version.ktor}"
    const val ktorJwt =  "io.ktor:ktor-auth-jwt:${Version.ktor}"
    const val ktorGson =  "io.ktor:ktor-gson:${Version.ktor}"
    const val ktorClient = "io.ktor:ktor-client-apache:${Version.ktor}"

    const val logback = "ch.qos.logback:logback-classic:${Version.logback}"

    const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlin}"
}
