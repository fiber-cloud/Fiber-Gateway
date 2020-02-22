package app.fiber.service

import app.fiber.service.selector.Selector
import io.ktor.application.ApplicationCall
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.HttpStatement
import io.ktor.features.origin
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.request.httpMethod
import io.ktor.request.queryString
import io.ktor.request.receiveText
import io.ktor.request.uri
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * Defines a service in favor of forwarding.
 *
 * @property [name] Name of the [Service].
 * @property [selector] Selector to match this [Service].
 * @property [host] Host of the [Service].
 * @property [port] Port of the [Service].
 *
 * @author Tammo0987
 * @since 1.0
 */
data class Service(
    private val name: String,
    val selector: Selector,
    private val host: String = System.getenv("${name.toUpperCase()}_SERVICE_HOST"),
    private val port: String = System.getenv("${name.toUpperCase()}_SERVICE_PORT")
) : KoinComponent {

    /**
     * Forward the [call] to the service and get the response.
     *
     * @param [call] [ApplicationCall] which holds the request information.
     *
     * @return [HttpStatement] of the call.
     */
    suspend fun forward(call: ApplicationCall): HttpStatement {
        val client by inject<HttpClient>()

        val request = call.request
        return client.request {
            url {
                host = this@Service.host
                port = this@Service.port.toInt()
                encodedPath = request.uri.replace(request.queryString(), "")
                protocol = URLProtocol.byName[request.origin.scheme] ?: URLProtocol.HTTP
                parameters.appendAll(request.queryParameters)
            }
            method = HttpMethod(request.httpMethod.value.toUpperCase())
            headers.appendAll(request.headers)
            body = call.receiveText()
        }
    }

}