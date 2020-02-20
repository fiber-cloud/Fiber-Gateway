package app.fiber

import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals

class GatewayTest {

    @Test
    fun `test internal server error log`(): Unit = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Post, "/api/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }.apply {
            assertTrue(this.requestHandled)
            assertEquals(HttpStatusCode.InternalServerError, this.response.status())
            assertTrue(this.response.content!!.contains("Receiving null values is not supported"))
        }
    }

}