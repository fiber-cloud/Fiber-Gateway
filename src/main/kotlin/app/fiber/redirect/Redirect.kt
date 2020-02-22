package app.fiber.redirect

import app.fiber.service.Service
import app.fiber.service.ServiceRepository
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.statement.readBytes
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.util.appendFiltered
import io.ktor.util.pipeline.PipelineContext
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
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

        response.execute { clientResponse ->
            call.respond(object : OutgoingContent.WriteChannelContent() {

                override val contentLength: Long? = clientResponse.contentLength()

                override val contentType: ContentType? = clientResponse.contentType()

                override val headers: Headers = Headers.build {
                    appendFiltered(clientResponse.headers) { key, _ ->
                        !key.equals(HttpHeaders.ContentType, ignoreCase = true) &&
                                !key.equals(HttpHeaders.ContentLength, ignoreCase = true)
                    }
                }

                override val status: HttpStatusCode? = clientResponse.status

                override suspend fun writeTo(channel: ByteWriteChannel) = channel.writeFully(clientResponse.readBytes())

            })
        }
    }

}