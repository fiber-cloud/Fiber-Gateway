package app.fiber

import app.fiber.service.ServiceRepository
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.ContentType
import io.ktor.http.withCharset
import io.ktor.response.respond
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.dsl.module

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)){}.start(true)
}

fun Application.main() {
    install(org.koin.ktor.ext.Koin) {
        modules(gatewayModule)
    }

    install(io.ktor.features.DefaultHeaders)
    install(io.ktor.features.CallLogging)
    install(app.fiber.feature.Forwarding)

    install(io.ktor.features.StatusPages) {
        exception<Exception> {
            val writer = java.io.StringWriter()
            it.printStackTrace(java.io.PrintWriter(writer))

            call.respond(
                io.ktor.http.content.TextContent(
                    writer.toString(),
                    ContentType.Text.Plain.withCharset(kotlin.text.Charsets.UTF_8),
                    io.ktor.http.HttpStatusCode.InternalServerError
                )
            )
        }
    }
}

val gatewayModule = module {
    single { ServiceRepository() }
}