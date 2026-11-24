package com.communityvoice.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import kotlin.math.pow

/**
 * The categories a community issue can belong to.
 * These exact names (uppercase) are what the Android app sends and receives.
 */
enum class Category {
    ROADS,
    WATER,
    ELECTRICITY,
    SANITATION,
    HEALTH,
    SCHOOLS,
    SAFETY,
    OTHER
}

/**
 * Where an issue has got to. Only an OFFICIAL or ADMIN can move it along -
 * citizens always create issues as OPEN.
 */
enum class IssueStatus {
    OPEN,
    UNDER_REVIEW,
    IN_PROGRESS,
    RESOLVED,
    /** Looked at and will not be acted on. The note explains why. */
    DECLINED
}

/** Which way somebody voted on an issue. */
enum class VoteStance {
    SUPPORT,
    DECLINE
}

/**
 * One entry in an issue's history - "moved to In progress on 3 March".
 * Stored inside the issue document, so reading an issue reads its whole story.
 */
data class StatusUpdate(
    val status: IssueStatus,
    val note: String = "",
    val updatedById: String = "",
    val updatedByName: String = "",
    val updatedAt: Instant = Instant.now()
)

/**
 * One community issue, as stored in the MongoDB "issues" collection.
 *
 * [supporterIds] and [declinerIds] hold the user IDs that already voted, which
 * is how one-vote-per-person is enforced. Neither list is sent back to the app -
 * see IssueResponse. The app only ever learns the counts plus its own vote.
 */
@Document(collection = "issues")
@CompoundIndexes(
    // The feed's two default orderings.
    CompoundIndex(name = "support_created_idx", def = "{'supportCount': -1, 'createdAt': -1}"),
    // "Issues I posted" and "issues in my area", which the profile screen uses.
    CompoundIndex(name = "author_created_idx", def = "{'authorId': 1, 'createdAt': -1}")
)
data class Issue(
    @Id
    val id: String? = null,

    val title: String,

    val description: String,

    val category: Category,

    val status: IssueStatus = IssueStatus.OPEN,

    /** Optional free text like "Market Road, near the junction". */
    val location: String = "",

    // ---- who raised it -------------------------------------------------
    // The author's name and avatar colour are copied onto the issue so the
    // feed can be drawn from one query instead of one lookup per row. If a
    // user renames themselves, AuthService fixes up their old issues.

    @Indexed
    val authorId: String,

    val authorName: String,

    val authorAvatarColor: String = "#1B6B4A",

    val authorRole: UserRole = UserRole.CITIZEN,

    // ---- the vote tally ------------------------------------------------

    val supportCount: Int = 0,

    val declineCount: Int = 0,

    val supporterIds: Set<String> = emptySet(),

    val declinerIds: Set<String> = emptySet(),

    val commentCount: Int = 0,

    // ---- timestamps and history ---------------------------------------

    @Indexed
    val createdAt: Instant = Instant.now(),

    val updatedAt: Instant = Instant.now(),

    val statusHistory: List<StatusUpdate> = emptyList()
) {
    /**
     * Support minus declines. Can go negative.
     *
     * @Transient because this is worked out on the fly rather than stored -
     * without it Spring Data would write a stale copy into every document.
     */
    @get:Transient
    val netScore: Int
        get() = supportCount - declineCount

    /**
     * "Trending" ranking: strong recent feeling beats a big old pile of votes.
     *
     * The shape is the classic one used by news aggregators - score over age,
     * with age raised to a power so interest decays smoothly. The +2 keeps a
     * brand-new issue from dividing by something close to zero, and the +1 on
     * the score means a fresh issue with no votes yet still appears.
     */
    fun trendingScore(now: Instant = Instant.now()): Double {
        val hoursOld = (now.toEpochMilli() - createdAt.toEpochMilli())
            .coerceAtLeast(0L) / 3_600_000.0
        val engagement = netScore + (commentCount * 0.5) + 1.0
        return engagement / (hoursOld + 2.0).pow(1.5)
    }
}
