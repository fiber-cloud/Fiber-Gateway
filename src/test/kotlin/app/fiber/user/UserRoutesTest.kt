package app.fiber.user

import app.fiber.database.User
import app.fiber.main
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.content
import kotlinx.serialization.json.json
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.util.*
import kotlin.test.assertEquals

class UserRoutesTest {

    private val testUser = User(UUID.randomUUID(), "Name", "Password")

    @Rule
    @JvmField
    val environmentVariables = EnvironmentVariables()

    @Before
    fun setUp() {
        this.environmentVariables.set("SECRET_JWT", "SECRET")
    }

    @Test
    fun `test if user was created`() = testApp {
        val userRepository = mockk<UserRepository>()

        every { userRepository.addUser(testUser) } just Runs
        every { userRepository.getUserByName("Name") } returns testUser
        every { userRepository.getUserById(testUser.uuid.toString()) } returns testUser

        loadKoinModules(
            module {
                single(override = true) { userRepository }
            }
        )

        handleRequestWithAuth(HttpMethod.Post, "/api/user") {
            setBody(Gson().toJson(testUser))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }.apply {
            assertTrue(this.requestHandled)
            assertEquals(HttpStatusCode.Created, this.response.status())

            verify {
                userRepository.addUser(testUser)
            }
        }
    }

    @Test
    @ImplicitReflectionSerializer
    fun `test if user was deleted`() = testApp {
        val userRepository = mockk<UserRepository>()

        every { userRepository.deleteUser(testUser) } just Runs
        every { userRepository.getUserByName("Name") } returns testUser
        every { userRepository.getUserById(testUser.uuid.toString()) } returns testUser

        loadKoinModules(
            module {
                single(override = true) { userRepository }
            }
        )

        handleRequestWithAuth(HttpMethod.Delete, "/api/user") {
            setBody(Gson().toJson(testUser))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }.apply {
            assertTrue(this.requestHandled)
            assertEquals(HttpStatusCode.OK, this.response.status())

            verify {
                userRepository.deleteUser(testUser)
            }
        }
    }

    /**
     * Private method used to reduce boilerplate when testing the application.
     */
    private fun testApp(callback: TestApplicationEngine.() -> Unit) {
        withTestApplication(Application::main) { callback() }
    }

    private fun TestApplicationEngine.handleRequestWithAuth(
        method: HttpMethod,
        uri: String,
        setup: TestApplicationRequest.() -> Unit = {}
    ) = handleRequest(method, uri) {
        val response = handleRequest(HttpMethod.Post, "/api/login") {
            val body = json {
                "name" to "Name"
                "password" to "Password"
            }.toString()

            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(body)
        }.response.content

        val token = Json(JsonConfiguration.Stable).parseJson(response!!).jsonObject["token"]?.content

        addHeader(HttpHeaders.Authorization, "Bearer $token")
        setup()
    }

}