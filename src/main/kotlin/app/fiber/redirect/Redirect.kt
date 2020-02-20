package app.fiber.redirect

import app.fiber.service.Service
import app.fiber.service.ServiceRepository
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.util.filter
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.copyAndClose
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * Handles the redirect to other services in the cluster.
 *
 * @author Tammo0987
 * @since 1.0
 */
class Redirect : KoinComponent {

    /**
     * [ServiceRepository] for getting the matching [services][Service].
     */
    private val serviceRepository by inject<ServiceRepository>()

    /**
     * Hooks the incoming requests and forward them to the matching [Service] and send the response back.
     *
     * @param [context] [PipelineContext] for hooking the incoming requests.
     */
    suspend fun redirect(context: PipelineContext<Unit, ApplicationCall>) {
        val call = context.call
        val service = this.serviceRepository.select(call.request.uri)

        val response = service.forward(call)

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