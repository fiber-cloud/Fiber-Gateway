package app.fiber

import app.fiber.authentication.JwtConfiguration
import app.fiber.authentication.authenticate
import app.fiber.cache.KubernetesPodCacheInvalidator
import app.fiber.cache.PodCacheInvalidator
import app.fiber.database.CassandraUserDatabase
import app.fiber.database.UserDatabase
import app.fiber.logger.logger
import app.fiber.redirect.Redirect
import app.fiber.service.ServiceRepository
import app.fiber.user.UserRepository
import app.fiber.user.user
import com.datastax.oss.driver.api.core.AllNodesFailedException
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import java.io.PrintWriter
import java.io.StringWriter
import java.net.InetSocketAddress

/**
 * Main function to start the ktor server and the application.
 *
 * @author Tammo0987
 * @since 1.0
 */
fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(true)
}

/**
 * [Koin] module for dependency injection.
 */
val gatewayModule = module {
    val logger by logger()

    val cassandraHost = System.getenv("CASSANDRA_SERVICE_HOST") ?: "".also {
        logger.error("Cassandra host not found!")
    }

    val jwtSecret = System.getenv("SECRET_JWT") ?: "".also {
        logger.error("Secret for JWT not found!")
    }

    val session: CqlSession? = try {
        CqlSessionBuilder()
            .addContactPoint(InetSocketAddress(cassandraHost, 9042))
            .withLocalDatacenter("datacenter1")
            .build()
    } catch (e: AllNodesFailedException) {
        logger.error("Could not connect to Cassandra", e.message)
        null
    }

    single<UserDatabase> { CassandraUserDatabase(session) }

    single { JwtConfiguration(jwtSecret) }

    val client = HttpClient()
    Runtime.getRuntime().addShutdownHook(Thread(client::close))

    single { client }

    single { UserRepository() }
    single { ServiceRepository() }
    single { DefaultKubernetesClient() }
    single<PodCacheInvalidator> { KubernetesPodCacheInvalidator() }
}

/**
 * Main [Application] module for ktor.
 */
fun Application.main() {
    install(Koin) {
        modules(gatewayModule)
    }

    install(DefaultHeaders)
    install(CallLogging)

    install(Authentication) {
        jwt {
            val userRepository by inject<UserRepository>()
            val jwtConfiguration by inject<JwtConfiguration>()

            realm = "Fiber-Gateway"

            verifier(jwtConfiguration.verifier)
            validate { credential ->
                if (userRepository.getUserById(credential.payload.claims["user_id"]!!.asString()) != null) JWTPrincipal(
                    credential.payload
                ) else null
            }
        }
    }

    install(CORS) {
        anyHost()
        allowNonSimpleContentTypes = true
    }

    install(StatusPages) {
        exception<Exception> {
            val writer = StringWriter()
            it.printStackTrace(PrintWriter(writer))

            this.call.respond(
                TextContent(
                    writer.toString(),
                    ContentType.Text.Plain.withCharset(Charsets.UTF_8),
                    HttpStatusCode.InternalServerError
                )
            )
        }
    }

    install(Routing) {
        route("/api") {
            install(ContentNegotiation) {
                gson()
            }

            authenticate()

            authenticate {
                user()
            }
        }

        authenticate {
            val redirect = Redirect()
            route("*") {
                handle { redirect.redirect(this) }
            }
        }
    }
}