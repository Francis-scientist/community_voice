package com.communityvoice.backend.security

import com.communityvoice.backend.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Runs before every request and works out who is calling.
 *
 * The app sends its login token in a standard header:
 *
 *     Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 *
 * If that token is valid and still names a real account, the matching
 * [AuthUser] is attached to the request. If not, nothing is attached and the
 * request carries on as an anonymous visitor.
 *
 * This filter never rejects anything by itself. Deciding whether a particular
 * endpoint *requires* a user is CurrentUserArgumentResolver's job, which keeps
 * the rule next to the endpoint that needs it instead of in a URL pattern list
 * that quietly drifts out of date.
 */
@Component
class AuthTokenFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    companion object {
        /** Where the resolved user is stashed for the rest of the request. */
        const val ATTRIBUTE = "communityvoice.currentUser"

        private const val HEADER = "Authorization"
        private const val PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        resolveUser(request)?.let { request.setAttribute(ATTRIBUTE, it) }
        filterChain.doFilter(request, response)
    }

    private fun resolveUser(request: HttpServletRequest): AuthUser? {
        val header = request.getHeader(HEADER) ?: return null
        if (!header.startsWith(PREFIX, ignoreCase = true)) return null

        val token = header.substring(PREFIX.length).trim()
        if (token.isEmpty()) return null

        // Invalid or expired tokens come back as null and are ignored.
        val payload = jwtService.decode(token) ?: return null

        // Look the account up every time rather than trusting the token's copy
        // of the name and role. That way a deleted account, a rename or a
        // promotion to OFFICIAL takes effect on the very next request instead
        // of whenever the old token happens to expire.
        val user = userRepository.findById(payload.userId).orElse(null) ?: return null
        return AuthUser.from(user)
    }
}
