package app.fiber.service.selector

/**
 * Enumeration of all [SelectorTypes][SelectorType].
 *
 * @param [select] [Selection] for matching to a pattern.
 *
 * @author Tammo0987
 * @since 1.0
 */
enum class SelectorType(val select: Selection) {

    STARTS_WITH({ route, pattern -> route.startsWith(pattern) }),
    ENDS_WITH({ route, pattern -> route.endsWith(pattern) }),
    CONTAINS({ route, pattern -> route.contains(pattern) })

}

/**
 * Only a type alias for a higher order functions, which passes a route and a pattern, and returns if the route is
 * matching.
 *
 * @author Tammo0987
 * @since 1.0
 */
typealias Selection = (route: String, pattern: String) -> Boolean