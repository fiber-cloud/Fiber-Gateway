package app.fiber.authentication

import app.fiber.user.UserRepository
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.patch
import io.ktor.routing.post
import org.koin.ktor.ext.inject
import java.util.*

/**
 * Routes for login and invalidate the cache.
 *
 * @receiver [Route]
 *
 * @author Tammo0987
 * @since 1.0
 */
fun Route.authenticate() {
    login()
    invalidateCache()
}

/**
 * Route for login with credentials.
 *
 * @receiver [Route]
 *
 * @author Tammo0987
 * @since 1.0
 */
private fun Route.login() {
    val userRepository by inject<UserRepository>()
    val jwtConfiguration by inject<JwtConfiguration>()

    post("/login") {
        val credentials = this.call.receive<UserCredentials>()
        val user = userRepository.getUserByName(credentials.name)

        if (user == null) {
            this.call.respond(HttpStatusCode.NotFound, LoginResponse(false))
        } else {
            val success = credentials.password == user.password

            if (success) {
                this.call.respond(LoginResponse(true, jwtConfiguration.makeToken(user.uuid.toString())))
            } else {
                this.call.respond(HttpStatusCode.Forbidden, LoginResponse(false))
            }
        }
    }
}

/**
 * Route for invalidate the cache for a specific uuid.
 *
 * @receiver [Route]
 *
 * @author Tammo0987
 * @since 1.0
 */
private fun Route.invalidateCache() {
    val userRepository by inject<UserRepository>()

    patch("/cache/remove/{uuid}") {
        val uuid = UUID.fromString(this.call.parameters["uuid"])
        userRepository.invalidateCache(uuid)

        this.call.respond(HttpStatusCode.OK)
    }
}

/**
 * Credential data class for content negotiation.
 *
 * @param [name] Name of the user.
 * @param [password] Hashed password of the user.
 *
 * @author Tammo0987
 * @since 1.0
 */
data class UserCredentials(val name: String, val password: String)

/**
 * LoginResponse data class for content negotiation.
 *
 * @param [success] If login has succeeded.
 * @param [token] Token value if present.
 *
 * @author Tammo0987
 * @since 1.0
 */
data class LoginResponse(val success: Boolean, val token: String = "")