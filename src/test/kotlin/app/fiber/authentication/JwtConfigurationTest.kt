package app.fiber.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.SignatureVerificationException
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class JwtConfigurationTest {

    private val secret = "SECRET"

    private lateinit var jwtConfiguration: JwtConfiguration

    @Before
    fun setUp() {
        this.jwtConfiguration = JwtConfiguration(this.secret)
    }

    @Test(expected = SignatureVerificationException::class)
    fun `token verify should fail`() {
        val algorithm = Algorithm.HMAC256("NOT THE SECRET")
        val verifier: JWTVerifier = JWT
            .require(algorithm)
            .withIssuer("Fiber-Gateway")
            .acceptExpiresAt(5)
            .build()

        val token = this.jwtConfiguration.makeToken(UUID.randomUUID().toString())
        verifier.verify(token)
    }

    @Test
    fun `generate and verify token`() {
        val verifier = this.jwtConfiguration.verifier

        val userId = UUID.randomUUID().toString()
        val token = this.jwtConfiguration.makeToken(userId)

        val verifiedToken = verifier.verify(token)

        assertEquals(verifiedToken.subject, "Authentication")
        assertEquals(verifiedToken.issuer, "Fiber-Gateway")
        assertEquals(verifiedToken.claims.getValue("user_id").asString(), userId)
        assertTrue(verifiedToken.expiresAt.time > System.currentTimeMillis())

        val algorithm = Algorithm.HMAC256(this.secret)
        assertEquals(verifiedToken.algorithm, algorithm.name)

        algorithm.verify(verifiedToken)
    }

}