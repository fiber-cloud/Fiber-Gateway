package app.fiber.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Responsible for the authentication and authorization of the whole application.
 *
 * @author Tammo0987
 * @since 1.0
 */
object JwtConfiguration {

    /**
     * Issuer for the generated JWT tokens.
     *
     * @see verifier
     * @see makeToken
     */
    private const val issuer = "Fiber-Gateway"

    /**
     * Secret for signing the JWT tokens.
     *
     * @see algorithm
     * @throws SecretNotAvailableException If secret is not in the environment variables.
     */
    private val secret = System.getenv("SECRET_JWT") ?: throw SecretNotAvailableException()

    /**
     * Algorithm for creating the JWT tokens.
     *
     * @see verifier
     * @see makeToken
     */
    private val algorithm = Algorithm.HMAC256(this.secret)

    /**
     * Verifier object for verify access to the application.
     */
    val verifier: JWTVerifier = JWT
        .require(this.algorithm)
        .withIssuer(this.issuer)
        .acceptExpiresAt(5)
        .build()

    /**
     * Generate a JWT token for a specific [userId].
     *
     * @param [userId] Id of user, who owns the token.
     * @return The generated JWT token.
     */
    fun makeToken(userId: String): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(this.issuer)
        .withClaim("id", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)))
        .sign(this.algorithm)

}

/**
 * Should be thrown, if the JWT secret was not found in the environment variables.
 *
 * @author Tammo0987
 * @since 1.0
 */
class SecretNotAvailableException : Exception("JWT Secret is not available!")