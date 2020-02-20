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
class JwtConfiguration(private val secret: String) {

    /**
     * Issuer for the generated JWT tokens.
     *
     * @see verifier
     * @see makeToken
     */
    private val issuer = "Fiber-Gateway"

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
        .withClaim("user_id", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)))
        .sign(this.algorithm)

}