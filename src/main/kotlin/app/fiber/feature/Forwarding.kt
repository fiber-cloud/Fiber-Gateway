package app.fiber.feature

import app.fiber.service.ServiceRepository
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.filter
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinComponent
import org.koin.core.inject

class Forwarding : KoinComponent {

    private val client: HttpClient = HttpClient()

    private val serviceRepository by inject<ServiceRepository>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread(this.client::close))
    }

    class Configuration

    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val call = context.call
        val service = serviceRepository.select(call.request.uri)

        runBlocking {
            val response = service.forward(client, call)

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

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Forwarding> {

        override val key = AttributeKey<Forwarding>("Forwarding")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Forwarding {
            val feature = Forwarding()

            pipeline.intercept(ApplicationCallPipeline.Call) {
                feature.intercept(this)
            }

            return feature
        }
    }

}