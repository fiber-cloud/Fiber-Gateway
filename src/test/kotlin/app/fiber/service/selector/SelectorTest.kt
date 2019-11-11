package app.fiber.service.selector

import org.junit.Test

import org.junit.Assert.*

class SelectorTest {

    @Test
    fun `test starts_with selector type`() {
        val selector = Selector("test", SelectorType.STARTS_WITH)

        assertTrue(selector.isSelected("test/login"))
        assertFalse(selector.isSelected("/test/login"))
        assertFalse(selector.isSelected("/login/test"))
    }

    @Test
    fun `test contains selector type`() {
        val selector = Selector("test", SelectorType.CONTAINS)

        assertTrue(selector.isSelected("/test/login"))
        assertTrue(selector.isSelected("test/login"))
        assertTrue(selector.isSelected("/login/test"))
        assertFalse(selector.isSelected("/login"))
    }

    @Test
    fun `test ends_with selector type`() {
        val selector = Selector("test", SelectorType.ENDS_WITH)

        assertTrue(selector.isSelected("/login/test"))
        assertFalse(selector.isSelected("/login/test/"))
        assertFalse(selector.isSelected("/test/login"))
        assertFalse(selector.isSelected("test/login"))
    }

}