package app.fiber.authentication

import app.fiber.user.UserRepository
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import org.koin.ktor.ext.inject
import java.util.*

fun Route.authenticate() {
    val userRepository by inject<UserRepository>()

    post("/login") {
        val credentials = this.call.receive<UserCredentials>()
        val user = userRepository.getUserByName(credentials.name)

        if (user == null) {
            this.call.respond(LoginResponse(false))
        } else {
            val success = credentials.password == user.password
            val token = if (success) JwtConfiguration.makeToken(user.uuid.toString()) else ""
            this.call.respond(LoginResponse(success, token))
        }
    }

    post("/cache/remove/{uuid}") {
        val uuid = UUID.fromString(this.call.parameters["uuid"])
        userRepository.invalidateCache(uuid)

        this.call.respond(HttpStatusCode.OK)
    }
}

data class UserCredentials(val name: String, val password: String)

data class LoginResponse(val success: Boolean, val token: String = "")