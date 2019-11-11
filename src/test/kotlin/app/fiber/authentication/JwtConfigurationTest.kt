package app.fiber.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.SignatureVerificationException
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class JwtConfigurationTest {

    @BeforeEach
    fun `mock environment variables`() {
        mockkStatic(System::class)
        every { System.getenv("SECRET_JWT") } returns "Secret"
    }

    @Test
    fun `token verify should fail`() {
        val algorithm = Algorithm.HMAC256("Not the right secret")
        val verifier: JWTVerifier = JWT
            .require(algorithm)
            .withIssuer("Fiber-Gateway")
            .acceptExpiresAt(5)
            .build()

        val token = JwtConfiguration.makeToken(UUID.randomUUID().toString())
        assertThrows(SignatureVerificationException::class.java) {
            verifier.verify(token)
        }
    }

    @Test
    fun `generate and verify token`() {
        val secret = System.getenv("SECRET_JWT")
        assertEquals(secret, "Secret")

        val verifier = JwtConfiguration.verifier

        val userId = UUID.randomUUID().toString()
        val token = JwtConfiguration.makeToken(userId)

        val verifiedToken = verifier.verify(token)

        assertEquals(verifiedToken.subject, "Authentication")
        assertEquals(verifiedToken.issuer, "Fiber-Gateway")
        assertEquals(verifiedToken.claims.getValue("id").asString(), userId)
        assertTrue(verifiedToken.expiresAt.time > System.currentTimeMillis())

        val algorithm = Algorithm.HMAC256(secret)
        assertEquals(verifiedToken.algorithm, algorithm.name)

        algorithm.verify(verifiedToken)
    }

}