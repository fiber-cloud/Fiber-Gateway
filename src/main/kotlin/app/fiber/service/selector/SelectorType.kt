package app.fiber.service.selector

enum class SelectorType(val selector: (url: String, pattern: String) -> Boolean) {

    STARTS_WITH({ url, pattern -> url.startsWith(pattern) }),
    ENDS_WITH({ url, pattern -> url.endsWith(pattern) }),
    CONTAINS({ url, pattern -> url.contains(pattern) })

}