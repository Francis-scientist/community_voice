package com.communityvoice.backend.security

import com.communityvoice.backend.config.AppProperties
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/** What a token turned out to contain, once we trusted the signature. */
data class TokenPayload(
    val userId: String,
    val expiresAt: Instant
)

/**
 * Makes and checks the login tokens.
 *
 * A token is a JSON Web Token: three dot-separated parts holding "who you are"
 * and "when this stops being valid", with a signature over both. Because the
 * signature uses a secret only this server knows, nobody can hand us a token
 * claiming to be somebody else - but anyone *can* read what is inside one, so
 * the token holds an ID and nothing private.
 */
@Service
class JwtService(properties: AppProperties) {

    private val log = LoggerFactory.getLogger(JwtService::class.java)

    private val validity: Duration = Duration.ofDays(properties.auth.tokenValidityDays)

    private val key: SecretKey = run {
        val configured = properties.auth.jwtSecret.trim()
        val raw = if (configured.isNotEmpty()) {
            configured
        } else {
            log.warn(
                "JWT_SECRET is not set, so a random signing key was generated. " +
                    "Everybody is signed out every time this server restarts. " +
                    "Set the JWT_SECRET environment variable to fix that."
            )
            UUID.randomUUID().toString() + UUID.randomUUID().toString()
        }
        // HS256 needs at least 32 bytes of key. Hashing whatever we were given
        // always produces exactly 32, so any length of secret is accepted.
        Keys.hmacShaKeyFor(MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()))
    }

    /** Builds a signed token for [userId], valid from now. */
    fun issueToken(userId: String): TokenPayload {
        val now = Instant.now()
        val expiry = now.plus(validity)
        return TokenPayload(userId = userId, expiresAt = expiry)
    }

    fun encode(payload: TokenPayload): String =
        Jwts.builder()
            .subject(payload.userId)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(payload.expiresAt))
            .signWith(key)
            .compact()

    /**
     * Checks the signature and the expiry date.
     * Returns null for anything we do not fully trust - a tampered token, an
     * expired one, or plain gibberish. Callers treat null as "not signed in".
     */
    fun decode(token: String): TokenPayload? = try {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        val subject = claims.subject
        if (subject.isNullOrBlank()) {
            null
        } else {
            TokenPayload(userId = subject, expiresAt = claims.expiration.toInstant())
        }
    } catch (expired: ExpiredJwtException) {
        null
    } catch (invalid: JwtException) {
        null
    } catch (malformed: IllegalArgumentException) {
        null
    }
}
