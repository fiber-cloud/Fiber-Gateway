package app.fiber.service

import app.fiber.database.User
import app.fiber.main
import app.fiber.service.selector.Selector
import app.fiber.user.UserRepository
import io.ktor.application.Application
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.util.*
import kotlin.test.assertEquals

class ServiceTest : KoinTest {

    @Rule
    @JvmField
    val environmentVariables = EnvironmentVariables()

    @Before
    fun setUp() {
        this.environmentVariables.set("SECRET_JWT", "SECRET")
    }

    @Test
    fun `test if forwarding works`(): Unit = withTestApplication(Application::main) {
        val userRepository = mockk<UserRepository>()
        val id = UUID.randomUUID()
        val user = User(id, "Name", "Password")

        every { userRepository.getUserByName("Name") } returns user
        every { userRepository.getUserById(id.toString()) } returns user

        loadKoinModules(
            module {
                single(override = true) { userRepository }
            }
        )

        val token = handleRequest(HttpMethod.Post, "/api/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"name\":\"Name\",\"password\":\"Password\"}")
        }.response.content
            ?.replace("{\"success\":true,\"token\":\"", "")
            ?.replace("\"}", "")


        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.toString()) {
                        "http://test.com:80/test" -> {
                            respond("Forwarded Content", HttpStatusCode.OK, headersOf("test", "value"))
                        }
                        else -> error("Unhandled url ${request.url}")
                    }
                }
            }
        }

        loadKoinModules(
            module {
                single(override = true) { client }
            }
        )

        val serviceRepository by inject<ServiceRepository>()
        val service = Service("test", Selector("/test"), "test.com", "80")

        serviceRepository.addService(service)

        handleRequest(HttpMethod.Get, "/test") {
            addHeader(HttpHeaders.Authorization, "Bearer $token")
        }.apply {
            assertTrue(this.requestHandled)
            assertEquals("Forwarded Content", this.response.content)
            assertEquals(HttpStatusCode.OK, this.response.status())
            assertEquals("value", this.response.headers["test"])
        }
    }

}