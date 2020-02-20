package app.fiber.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Extension for easy [Logger] access.
 *
 * @receiver [Any]
 *
 * @author Tammo0987
 * @since 1.0
 */
fun Any.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(this::class.java.name) }
}