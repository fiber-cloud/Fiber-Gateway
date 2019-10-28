package app.fiber

import app.fiber.service.Service
import app.fiber.service.ServiceRepository
import app.fiber.service.selector.Selector
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Exception

fun main() {
    val client = HttpClient()

    embeddedServer(Netty, 8080) {
        install(DefaultHeaders)
        install(CallLogging)

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

        install(Koin) {
            modules(gatewayModule)
        }

        val repository by inject<ServiceRepository>()
        repository.addService(Service("shop", Selector("/shop")))

        routing {
            get("/hello") {
                call.respondText("Hello from Ktor gateway!", ContentType.Text.Plain)
            }

            route("/*") {
                handle {
                    val service = repository.select(this.call.request.uri)
                    launch {
                        val response = service.forward(client, this@handle.call)
                        println(response.receive<String>())
                    }
                }
            }
        }

    }.start(true)

    client.close()
}

val gatewayModule = module {
    single { ServiceRepository() }
}