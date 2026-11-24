package com.communityvoice.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import kotlin.math.abs

/**
 * What a signed-in person is allowed to do.
 *
 *  - CITIZEN  - the default. Post issues, support or decline them, comment.
 *  - OFFICIAL - everything a citizen can do, plus move an issue along its
 *               lifecycle (Open -> Under review -> In progress -> Resolved).
 *               Think of a council worker who answers for these reports.
 *  - ADMIN    - everything, plus delete anybody's issue or comment.
 */
enum class UserRole {
    CITIZEN,
    OFFICIAL,
    ADMIN;

    /** Can this role change an issue's status? */
    val canModerateStatus: Boolean
        get() = this == OFFICIAL || this == ADMIN

    /** Can this role delete content that belongs to somebody else? */
    val canDeleteAnything: Boolean
        get() = this == ADMIN
}

/**
 * One registered person, as stored in the MongoDB "users" collection.
 *
 * [passwordHash] is a BCrypt hash - the plain password is never stored and
 * cannot be recovered from it. It is also never sent to the app; see
 * UserResponse in web/AuthDtos.kt for what does leave the server.
 */
@Document(collection = "users")
data class User(
    @Id
    val id: String? = null,

    val displayName: String,

    /**
     * Always stored lower-cased and trimmed, so "Ann@x.com" and "ann@x.com"
     * are the same account. The unique index is what actually stops two people
     * registering the same address.
     */
    @Indexed(unique = true)
    val email: String,

    val passwordHash: String,

    val role: UserRole = UserRole.CITIZEN,

    /** Free text the user can edit - "Market Road trader", that sort of thing. */
    val bio: String = "",

    /** Optional area name, shown on their profile and on their issues. */
    val neighbourhood: String = "",

    /** Hex colour for the initials avatar, picked once at registration. */
    val avatarColor: String = "#1B6B4A",

    @Indexed
    val createdAt: Instant = Instant.now(),

    val lastSeenAt: Instant = Instant.now()
) {
    companion object {
        /**
         * The palette the initials avatars are drawn from. Chosen so white
         * text stays readable on every one of them.
         */
        private val AVATAR_COLORS = listOf(
            "#1B6B4A", "#1E5F8C", "#8C4A1E", "#6B2D6B",
            "#2D6B6B", "#8C6B1E", "#4A3C8C", "#8C1E4A"
        )

        /** Stable colour for a person, so their avatar never changes. */
        fun avatarColorFor(seed: String): String =
            AVATAR_COLORS[abs(seed.hashCode()) % AVATAR_COLORS.size]

        /** "Amina Yusuf" -> "AY", "Pat" -> "P". Used for the avatar circle. */
        fun initialsFor(displayName: String): String {
            val words = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            return when {
                words.isEmpty() -> "?"
                words.size == 1 -> words[0].take(1).uppercase()
                else -> (words.first().take(1) + words.last().take(1)).uppercase()
            }
        }
    }
}
