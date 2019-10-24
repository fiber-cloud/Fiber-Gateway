package app.fiber

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, 8080) {
        install(DefaultHeaders)
        install(CallLogging)

        routing {
            get("/hello") {
                call.respondText("Hello from Ktor gateway!", ContentType.Text.Plain)
            }
        }

    }.start(true)

}