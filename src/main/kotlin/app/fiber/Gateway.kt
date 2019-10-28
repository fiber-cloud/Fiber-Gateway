package app.fiber

import app.fiber.service.ServiceRepository
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.filter
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import java.io.PrintWriter
import java.io.StringWriter

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

        val serviceRepository by inject<ServiceRepository>()

        intercept(ApplicationCallPipeline.Call) {
            val service = serviceRepository.select(this.call.request.uri)

            launch {
                val response = service.forward(client, this@intercept.call)
                val call = this@intercept.call

                call.respond(object : OutgoingContent.WriteChannelContent() {
                    override val contentType: ContentType? = ContentType.parse(
                        response.headers[HttpHeaders.ContentType] ?: ContentType.Text.Plain.toString()
                    )
                    override val headers: Headers = Headers.build {
                        appendAll(response.headers.filter { key, _ ->
                            !key.equals(
                                HttpHeaders.ContentType,
                                ignoreCase = true
                            ) && !key.equals(HttpHeaders.ContentLength, ignoreCase = true)
                        })
                    }
                    override val status: HttpStatusCode? = response.status
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        response.content.copyAndClose(channel)
                    }
                })
            }
        }

    }.start(true)

    client.close()
}

val gatewayModule = module {
    single { ServiceRepository() }
}