package app.fiber.service

import app.fiber.service.selector.Selector
import app.fiber.service.selector.SelectorType
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServiceRepositoryTest {

    private val testService = Service("test", Selector("test", SelectorType.STARTS_WITH), "test.com", "80")

    @Test
    fun `test if service could be add`() {
        val serviceRepository = ServiceRepository()
        assertTrue(serviceRepository.addService(this.testService))
    }

    @Test
    fun `test if select works`() {
        val serviceRepository = ServiceRepository()
        serviceRepository.addService(this.testService)

        val selectedService = serviceRepository.select("test")
        assertEquals(this.testService, selectedService)
    }

    @Test
    fun `test if select does not find service`() {
        val serviceRepository = ServiceRepository()
        assertFailsWith(IllegalArgumentException::class, "Service not found!") { serviceRepository.select("test") }
    }

    @Test
    fun `test if select find more than one service`() {
        val serviceRepository = ServiceRepository()
        serviceRepository.addService(this.testService)
        serviceRepository.addService(Service("test_2", Selector("test", SelectorType.STARTS_WITH), "test.com", "80"))

        assertFailsWith(
            IllegalArgumentException::class,
            "Found more than 1 selector!"
        ) { serviceRepository.select("test") }
    }

}