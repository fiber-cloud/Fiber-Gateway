package app.fiber

import app.fiber.authentication.JwtConfiguration
import app.fiber.authentication.authenticate
import app.fiber.cassandra.CassandraConnector
import app.fiber.feature.Forwarding
import app.fiber.service.ServiceRepository
import app.fiber.user.UserRepository
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
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

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(true)
}

fun Application.main() {
    install(Koin) {
        modules(gatewayModule)
    }

    val userRepository by inject<UserRepository>()

    install(DefaultHeaders)
    install(CallLogging)

    install(Authentication) {
        jwt {
            verifier(JwtConfiguration.verifier)
            validate { credential ->
                if (userRepository.checkId(credential.payload.id) != null) JWTPrincipal(credential.payload) else null
            }
        }
    }

    install(StatusPages) {
        exception<Exception> {
            val writer = StringWriter()
            it.printStackTrace(PrintWriter(writer))

            call.respond(
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
        }

        authenticate {
            install(Forwarding)
        }
    }
}

val gatewayModule = module {
    val cassandraHost = System.getenv("CASSANDRA_SERVICE_HOST") ?: throw Exception("Cassandra host not found!")

    val cassandra = CassandraConnector(cassandraHost)
    val userRepository = UserRepository(cassandra.session)

    single { cassandra }
    single { userRepository }
    single { ServiceRepository() }
}