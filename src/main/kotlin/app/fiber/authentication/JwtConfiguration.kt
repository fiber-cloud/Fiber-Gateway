package app.fiber.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.*
import java.util.concurrent.TimeUnit

object JwtConfiguration {

    private const val issuer = "Fiber-Gateway"

    private val secret = System.getenv("SECRET_JWT") ?: throw SecretNotAvailableException()

    private val algorithm = Algorithm.HMAC256(this.secret)

    val verifier: JWTVerifier = JWT
        .require(this.algorithm)
        .withIssuer(this.issuer)
        .acceptExpiresAt(5)
        .build()

    fun makeToken(userId: String): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(this.issuer)
        .withClaim("id", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)))
        .sign(this.algorithm)

}

class SecretNotAvailableException : Exception("JWT Secret is not available!")