package app.fiber.service

import app.fiber.service.selector.Selector
import io.ktor.application.ApplicationCall
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.response.HttpResponse
import io.ktor.features.origin
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.request.httpMethod
import io.ktor.request.queryString
import io.ktor.request.receiveText
import io.ktor.request.uri

/**
 * Defines a service in favor of forwarding.
 *
 * @param [name] Name of the [Service].
 * @param [selector] Selector to match this [Service].
 *
 * @author Tammo0987
 * @since 1.0
 */
data class Service(private val name: String, val selector: Selector) {

    /**
     * Host of the service.
     */
    private val host: String? = System.getenv("${this.name.toUpperCase()}_SERVICE_HOST")

    /**
     * Port of the service.
     */
    private val port: String? = System.getenv("${this.name.toUpperCase()}_SERVICE_PORT")

    /**
     * Forward the [call] to the service and get the response.
     *
     * @param [client] [HttpClient] to call the service.
     * @param [call] [ApplicationCall] which holds the request information.
     *
     * @return [HttpResponse] of the call.
     */
    suspend fun forward(client: HttpClient, call: ApplicationCall): HttpResponse {
        val request = call.request
        return client.call {
            url {
                host = this@Service.host!!
                port = this@Service.port!!.toInt()
                encodedPath = request.uri.replace(request.queryString(), "")
                protocol = URLProtocol.byName[request.origin.scheme] ?: URLProtocol.HTTP
                parameters.appendAll(request.queryParameters)
            }
            method = HttpMethod(request.httpMethod.value.toUpperCase())
            headers.appendAll(request.headers)
            body = call.receiveText()
        }.response
    }

    /**
     * Overrides the [toString] call.
     */
    override fun toString(): String {
        return "Service(name='$name', host='$host', port='$port')"
    }

}