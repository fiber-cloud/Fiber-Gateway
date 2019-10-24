package app.fiber.service.selector

data class Selector(private val pattern: String, private val type: SelectorType = SelectorType.STARTS_WITH) {

    fun isSelected(url: String): Boolean {
        return this.type.selector(url, this.pattern)
    }

}