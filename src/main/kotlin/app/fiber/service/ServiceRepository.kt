package app.fiber.service

/**
 * Repository, which is holding all instances of [services][Service] in [services].
 *
 * @author Tammo0987
 * @since 1.0
 */
class ServiceRepository {

    /**
     * [Set] of all instances.
     */
    private val services: MutableSet<Service> = HashSet()

    /**
     * Add the [service] to [services].
     *
     * @param [service] The [Service] which should be added.
     */
    fun addService(service: Service) {
        this.services.add(service);
    }

    /**
     * Find a [Service] by selecting through the selectors and the [uri] as parameter.
     *
     * @param [uri] Uri of the http request.
     *
     * @throws NullPointerException If [Service] was not found.
     * @throws IllegalArgumentException If more than 1 [Service] was found.
     * @return The [Service] if found.
     */
    fun select(uri: String): Service {
        val selected =  this.services.filter { it.selector.isSelected(uri) }

        require(selected.size <= 1) { "Found more than 1 selector!" }
        requireNotNull(selected.firstOrNull()) { "Service not found!" }

        return selected.first()
    }

}