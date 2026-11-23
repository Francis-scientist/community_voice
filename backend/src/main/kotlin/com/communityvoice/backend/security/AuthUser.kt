package com.communityvoice.backend.security

import com.communityvoice.backend.model.User
import com.communityvoice.backend.model.UserRole

/**
 * The signed-in person, as the controllers see them.
 *
 * This is a small read-only snapshot taken per request, not the stored [User]
 * document, so a controller cannot accidentally hand the password hash to
 * Jackson and leak it into a response.
 */
data class AuthUser(
    val id: String,
    val displayName: String,
    val email: String,
    val role: UserRole,
    val avatarColor: String
) {
    companion object {
        fun from(user: User): AuthUser = AuthUser(
            id = requireNotNull(user.id) { "a stored user always has an id" },
            displayName = user.displayName,
            email = user.email,
            role = user.role,
            avatarColor = user.avatarColor
        )
    }
}

/**
 * Put this on a controller method parameter to get the signed-in user.
 *
 *     fun createIssue(@CurrentUser user: AuthUser, ...)             // must be signed in
 *     fun listIssues(@CurrentUser(required = false) user: AuthUser?) // works either way
 *
 * With `required = true` (the default) a request without a valid token is
 * turned into HTTP 401 before the method body runs. With `required = false`
 * the parameter is simply null, which is how the feed can show vote counts to
 * a visitor while still highlighting *your* vote once you sign in.
 *
 * See CurrentUserArgumentResolver for the few lines that make this work.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUser(val required: Boolean = true)
