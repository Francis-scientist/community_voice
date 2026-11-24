package com.communityvoice.backend.web

import com.communityvoice.backend.security.AuthUser
import com.communityvoice.backend.security.CurrentUser
import com.communityvoice.backend.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Registering, signing in, and looking at profiles.
 *
 * Both register and login hand back a token. The app stores it and sends it as
 * `Authorization: Bearer <token>` on everything afterwards - see
 * security/AuthTokenFilter.kt for the other end of that.
 */
@RestController
@RequestMapping("/api")
class AuthController(private val authService: AuthService) {

    /** POST /api/auth/register - create an account and sign in. */
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        authService.register(request)

    /** POST /api/auth/login */
    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)

    /**
     * GET /api/me - who the current token belongs to.
     *
     * The app calls this on startup. A 401 here means the stored token has
     * expired or the account is gone, which is the app's cue to sign out
     * quietly and show the welcome screen.
     */
    @GetMapping("/me")
    fun me(@CurrentUser user: AuthUser): UserResponse =
        authService.currentProfile(user)

    /** PATCH /api/me - edit your own name, bio or neighbourhood. */
    @PatchMapping("/me")
    fun updateMe(
        @CurrentUser user: AuthUser,
        @Valid @RequestBody request: UpdateProfileRequest
    ): UserResponse = authService.updateProfile(user, request)

    /**
     * GET /api/users/{id} - anybody's public profile and contribution totals.
     * No token needed, and the response never includes their email address.
     */
    @GetMapping("/users/{id}")
    fun publicProfile(@PathVariable id: String): PublicProfileResponse =
        authService.publicProfile(id)
}
