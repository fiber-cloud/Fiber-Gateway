package app.fiber.user

import app.fiber.database.User
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.post
import org.koin.ktor.ext.inject

fun Route.user() {
    this.userAdd()
    this.userDelete()
}

private fun Route.userAdd() {
    val userRepository by inject<UserRepository>()

    post("/user") {
        val user = this.call.receive<User>()
        userRepository.addUser(user)

        this.call.respond(HttpStatusCode.Created)
    }
}

private fun Route.userDelete() {
    val userRepository by inject<UserRepository>()

    delete("/user") {
        val user = this.call.receive<User>()
        userRepository.deleteUser(user)

        this.call.respond(HttpStatusCode.OK)
    }
}