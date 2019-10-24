package app.fiber.service

import app.fiber.service.selector.Selector

data class Service(private val name: String, val selector: Selector) {

    private val host: String? = System.getenv("${this.name.toUpperCase()}_SERVICE_HOST")
    private val port: String? = System.getenv("${this.name.toUpperCase()}_SERVICE_PORT")

    override fun toString(): String {
        return "Service(name='$name', host='$host', port='$port')"
    }

}