package app.fiber.authentication

import app.fiber.database.User
import app.fiber.database.UserDatabase
import app.fiber.main
import app.fiber.user.UserRepository
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.*
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.util.*
import kotlin.test.assertEquals

class LoginRouteTest : KoinTest {

    private val userCredentialsJson = "{\"name\":\"Name\",\"password\":\"Password\"}"

    private val successFalse = "{\"success\":false,\"token\":\"\"}"

    @Rule
    @JvmField
    val environmentVariables = EnvironmentVariables()

    @Before
    fun setUp() {
        this.environmentVariables.set("SECRET_JWT", "SECRET")
    }

    @Test
    fun `test if user was not found`() = testApp {
        val userDatabase = mockk<UserDatabase>()
        every { userDatabase.getUserByName("Name") } returns null

        loadKoinModules(
            module {
                single(override = true) { userDatabase }
            }
        )

        handleRequest(HttpMethod.Post, "/api/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(userCredentialsJson)
        }.apply {
            assertTrue(this.requestHandled)
            assertEquals(HttpStatusCode.NotFound, this.response.status())
            assertEquals(successFalse, this.response.content)
        }
    }

    @Test
    fun `test login failure`() = testApp {
        val userDatabase = mockk<UserDatabase>()
        every { userDatabase.getUserByName("Name") } returns User(UUID.randomUUID(), "Name", "Wrong Password")

        loadKoinModules(
            module {
                single(override = true) { userDatabase }
            }
        )

        handleRequest(HttpMethod.Post, "/api/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(userCredentialsJson)
        }.apply {
            assertTrue(this.requestHandled)
            assertEquals(HttpStatusCode.Unauthorized, this.response.status())
            assertEquals(successFalse, this.response.content)
        }
    }

    @Test
    fun `test login success`() = testApp {
        val userDatabase = mockk<UserDatabase>()
        every { userDatabase.getUserByName("Name") } returns User(UUID.randomUUID(), "Name", "Password")

        loadKoinModules(
            module {
                single(override = true) { userDatabase }
            }
        )

        handleRequest(HttpMethod.Post, "/api/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(userCredentialsJson)
        }.apply {
            assertTrue(this.requestHandled)
            assertEquals(HttpStatusCode.OK, this.response.status())
            assertTrue(this.response.content!!.contains("\"success\":true"))
        }
    }

    @Test
    fun `test invalidate cache`() = testApp {
        val uuid = UUID.randomUUID()

        val userRepository = mockk<UserRepository>()
        every { userRepository.invalidateCache(uuid) } just Runs

        loadKoinModules(
            module {
                single(override = true) { userRepository }
            }
        )

        handleRequest(HttpMethod.Patch, "/api/cache/remove/$uuid").apply {
            assertTrue(this.requestHandled)
            assertEquals(HttpStatusCode.OK, this.response.status())

            verify {
                userRepository.invalidateCache(uuid)
            }
        }
    }

    /**
     * Private method used to reduce boilerplate when testing the application.
     */
    private fun testApp(callback: TestApplicationEngine.() -> Unit) {
        withTestApplication(Application::main) { callback() }
    }

}