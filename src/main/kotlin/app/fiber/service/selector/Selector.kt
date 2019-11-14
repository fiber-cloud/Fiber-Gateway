package app.fiber.service.selector

import app.fiber.service.Service

/**
 * Defines a [pattern] and a [type] to find matching [services][Service].
 *
 * @param [pattern] Pattern which has to be matching.
 * @param [type] Type of matching comparison.
 *
 * @author Tammo0987
 * @since 1.0
 */
data class Selector(private val pattern: String, private val type: SelectorType = SelectorType.STARTS_WITH) {

    /**
     * @return If the [route] is matching to the [pattern].
     */
    fun isSelected(route: String): Boolean {
        return this.type.select(route, this.pattern)
    }

}