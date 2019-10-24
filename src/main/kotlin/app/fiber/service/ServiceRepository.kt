package app.fiber.service

class ServiceRepository {

    private val services: MutableSet<Service> = HashSet()

    fun addService(service: Service) {
        this.services.add(service);
    }

    fun select(url: String): Service {
        val selected =  this.services.filter { it.selector.isSelected(url) }
        require(selected.size <= 1) { "Found more than 1 selector!" }

        return selected.first()
    }

}