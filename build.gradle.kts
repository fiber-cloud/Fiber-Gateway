import org.gradle.api.tasks.testing.logging.TestExceptionFormat
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

    implementation(Dependencies.kubernetes)

    implementation(Dependencies.logback)


    testImplementation(Dependencies.cassandraUnit)
    testImplementation(Dependencies.environmentVariables)
    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.koinTest)
    testImplementation(Dependencies.ktorClientMock)
    testImplementation(Dependencies.ktorClientMockJvm)
    testImplementation(Dependencies.ktorTestEngine)
    testImplementation(Dependencies.mockk)
}

tasks {

    test {
        jvmArgs = listOf("-Xmx4G", "-Xms4G")

        logging.captureStandardOutput(LogLevel.DEBUG)
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

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
        val path = "${buildDir.path}/export"

        into(path)

        val from = configurations.runtimeClasspath.get().filterNot { file(path).listFiles()?.map { file -> file.name }?.contains(it.name) ?: false }
        from(from)

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
    const val kubernetes = "4.7.1"

    const val cassandraUnit = "4.3.1.0"
    const val junit = "4.1.2"
    const val mockk = "1.9.3"
    const val environment = "1.19.0"
}

object Dependencies {
    const val cassandra = "com.datastax.oss:java-driver-core:${Version.cassandra}"
    const val cassandraQueryBuilder = "com.datastax.oss:java-driver-query-builder:${Version.cassandra}"

    const val koin = "org.koin:koin-ktor:${Version.koin}"

    const val ktorCore = "io.ktor:ktor-server-core:${Version.ktor}"

    const val ktorNetty = "io.ktor:ktor-server-netty:${Version.ktor}"
    const val ktorJwt =  "io.ktor:ktor-auth-jwt:${Version.ktor}"
    const val ktorGson = "io.ktor:ktor-gson:${Version.ktor}"
    const val ktorClient = "io.ktor:ktor-client-apache:${Version.ktor}"

    const val kubernetes = "io.fabric8:kubernetes-client:${Version.kubernetes}"

    const val logback = "ch.qos.logback:logback-classic:${Version.logback}"

    const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlin}"


    // Testing dependencies
    const val cassandraUnit = "org.cassandraunit:cassandra-unit:${Version.cassandraUnit}"

    const val environmentVariables = "com.github.stefanbirkner:system-rules:${Version.environment}"

    const val junit = "junit:junit:${Version.junit}"

    const val koinTest = "org.koin:koin-test:${Version.koin}"

    const val ktorClientMock = "io.ktor:ktor-client-mock:${Version.ktor}"
    const val ktorClientMockJvm = "io.ktor:ktor-client-mock-jvm:${Version.ktor}"

    const val ktorTestEngine = "io.ktor:ktor-server-test-host:${Version.ktor}"

    const val mockk = "io.mockk:mockk:${Version.mockk}"
}
