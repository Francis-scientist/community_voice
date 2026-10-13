package com.example.communityvoice.data

/**
 * Wire models - the exact shapes that travel between the app and the backend.
 *
 * Two rules hold throughout this file:
 *
 *  1. Every field has a default value. That makes Kotlin generate a
 *     no-argument constructor, which is what lets Gson fill in sensible
 *     values instead of nulls when the server omits a field.
 *
 *  2. Enum-ish fields (category, status, vote stance) are carried as plain
 *     Strings and mapped to the UI enums below. If a future server version
 *     adds a category this build has never heard of, an old app shows it as
 *     "Other" instead of crashing on an unknown enum constant.
 */

// --------------------------------------------------------------- categories

/**
 * The categories an issue can belong to, matching the backend's Category enum.
 *
 * [apiValue] is what travels over the network (uppercase).
 * [label] is what the user sees on screen.
 */
enum class IssueCategory(val apiValue: String, val label: String) {
    ROADS("ROADS", "Roads"),
    WATER("WATER", "Water"),
    ELECTRICITY("ELECTRICITY", "Electricity"),
    SANITATION("SANITATION", "Sanitation"),
    HEALTH("HEALTH", "Health"),
    SCHOOLS("SCHOOLS", "Schools"),
    SAFETY("SAFETY", "Safety"),
    OTHER("OTHER", "Other");

    companion object {
        /** Unknown values fall back to OTHER rather than throwing. */
        fun fromApi(apiValue: String?): IssueCategory =
            entries.firstOrNull { it.apiValue.equals(apiValue, ignoreCase = true) } ?: OTHER

        /** Turns "WATER" from the server into "Water" for display. */
        fun labelFor(apiValue: String?): String = fromApi(apiValue).label
    }
}

// ------------------------------------------------------------------ statuses

/**
 * Where an issue has got to. Citizens always create issues as OPEN; only a
 * verified official can move one along.
 */
enum class IssueStatus(val apiValue: String, val label: String) {
    OPEN("OPEN", "Open"),
    UNDER_REVIEW("UNDER_REVIEW", "Under review"),
    IN_PROGRESS("IN_PROGRESS", "In progress"),
    RESOLVED("RESOLVED", "Resolved"),
    DECLINED("DECLINED", "Not planned");

    /** True once the issue needs no more community pressure. */
    val isClosed: Boolean
        get() = this == RESOLVED || this == DECLINED

    companion object {
        fun fromApi(apiValue: String?): IssueStatus =
            entries.firstOrNull { it.apiValue.equals(apiValue, ignoreCase = true) } ?: OPEN
    }
}

/** Which way somebody voted. */
enum class VoteStance(val apiValue: String) {
    SUPPORT("SUPPORT"),
    DECLINE("DECLINE");

    companion object {
        /** null means "has not voted", which is a real and common state. */
        fun fromApi(apiValue: String?): VoteStance? =
            entries.firstOrNull { it.apiValue.equals(apiValue, ignoreCase = true) }
    }
}

/** What a member is allowed to do. Drives the "Official" badge in the UI. */
enum class MemberRole(val apiValue: String, val label: String) {
    CITIZEN("CITIZEN", "Member"),
    OFFICIAL("OFFICIAL", "Official"),
    ADMIN("ADMIN", "Admin");

    val canModerateStatus: Boolean
        get() = this == OFFICIAL || this == ADMIN

    companion object {
        fun fromApi(apiValue: String?): MemberRole =
            entries.firstOrNull { it.apiValue.equals(apiValue, ignoreCase = true) } ?: CITIZEN
    }
}

// --------------------------------------------------------------------- feed

/** The author of an issue or comment, as shown on the card. */
data class Author(
    val id: String = "",
    val displayName: String = "Someone",
    val initials: String = "?",
    val avatarColor: String = "#1B6B4A",
    val role: String = MemberRole.CITIZEN.apiValue
) {
    val memberRole: MemberRole get() = MemberRole.fromApi(role)
}

/** One line of an issue's official history. */
data class StatusUpdate(
    val status: String = IssueStatus.OPEN.apiValue,
    val note: String = "",
    val updatedByName: String = "",
    val updatedAt: String = ""
) {
    val issueStatus: IssueStatus get() = IssueStatus.fromApi(status)
}

/** One issue as returned by GET /api/issues and GET /api/issues/{id}. */
data class Issue(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = IssueCategory.OTHER.apiValue,
    val status: String = IssueStatus.OPEN.apiValue,
    val location: String = "",
    val author: Author = Author(),
    val supportCount: Int = 0,
    val declineCount: Int = 0,
    val netScore: Int = 0,
    val commentCount: Int = 0,
    /** The viewer's own vote, or null if they have not voted / are a guest. */
    val myVote: String? = null,
    /** True when the viewer raised this issue, so we can offer Delete. */
    val authoredByViewer: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val statusHistory: List<StatusUpdate> = emptyList()
) {
    val issueCategory: IssueCategory get() = IssueCategory.fromApi(category)
    val issueStatus: IssueStatus get() = IssueStatus.fromApi(status)
    val stance: VoteStance? get() = VoteStance.fromApi(myVote)

    /** Everyone who expressed a view, either way. */
    val totalVotes: Int get() = supportCount + declineCount

    /** Share of votes that were in support, 0f..1f. 0.5f when nobody voted. */
    val supportRatio: Float
        get() = if (totalVotes == 0) 0.5f else supportCount.toFloat() / totalVotes
}

/** Body for POST /api/issues */
data class CreateIssueRequest(
    val title: String = "",
    val description: String = "",
    val category: String = IssueCategory.OTHER.apiValue,
    val location: String = ""
)

/** Body for PUT /api/issues/{id}/vote */
data class VoteRequest(
    val stance: String = VoteStance.SUPPORT.apiValue
)

/** Body for PATCH /api/issues/{id}/status */
data class UpdateStatusRequest(
    val status: String = IssueStatus.OPEN.apiValue,
    val note: String = ""
)

// ----------------------------------------------------------------- comments

data class Comment(
    val id: String = "",
    val issueId: String = "",
    val body: String = "",
    val author: Author = Author(),
    val authoredByViewer: Boolean = false,
    val createdAt: String = ""
)

data class CreateCommentRequest(
    val body: String = ""
)

// --------------------------------------------------------------------- auth

data class RegisterRequest(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val neighbourhood: String = ""
)

data class LoginRequest(
    val email: String = "",
    val password: String = ""
)

data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val neighbourhood: String? = null
)

/** A member. [email] is only ever filled in for the signed-in user. */
data class Member(
    val id: String = "",
    val displayName: String = "",
    val email: String? = null,
    val role: String = MemberRole.CITIZEN.apiValue,
    val initials: String = "?",
    val avatarColor: String = "#1B6B4A",
    val bio: String = "",
    val neighbourhood: String = "",
    val joinedAt: String = ""
) {
    val memberRole: MemberRole get() = MemberRole.fromApi(role)
}

/** What register and login hand back. */
data class AuthResponse(
    val token: String = "",
    val expiresAt: String = "",
    val user: Member = Member()
)

/** GET /api/users/{id} */
data class PublicProfile(
    val user: Member = Member(),
    val issuesPosted: Long = 0,
    val commentsPosted: Long = 0,
    val supportReceived: Int = 0,
    val issuesResolved: Int = 0
)

// -------------------------------------------------------------------- stats

data class CategoryStat(
    val category: String = IssueCategory.OTHER.apiValue,
    val issueCount: Long = 0,
    val supportCount: Long = 0
) {
    val issueCategory: IssueCategory get() = IssueCategory.fromApi(category)
}

data class StatusStat(
    val status: String = IssueStatus.OPEN.apiValue,
    val issueCount: Long = 0
) {
    val issueStatus: IssueStatus get() = IssueStatus.fromApi(status)
}

data class ContributorStat(
    val userId: String = "",
    val displayName: String = "",
    val initials: String = "?",
    val avatarColor: String = "#1B6B4A",
    val issuesPosted: Long = 0,
    val supportReceived: Long = 0
)

data class CommunityStats(
    val totalIssues: Long = 0,
    val totalMembers: Long = 0,
    val totalSupports: Long = 0,
    val totalDeclines: Long = 0,
    val totalComments: Long = 0,
    val openIssues: Long = 0,
    val inProgressIssues: Long = 0,
    val resolvedIssues: Long = 0,
    val resolutionRatePercent: Int = 0,
    val issuesThisWeek: Long = 0,
    val byCategory: List<CategoryStat> = emptyList(),
    val byStatus: List<StatusStat> = emptyList(),
    val topContributors: List<ContributorStat> = emptyList(),
    val generatedAt: String = ""
)

// ------------------------------------------------------------------- paging

/** One page of a longer list. Keep asking for page + 1 while [hasMore]. */
data class PagedResponse<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
    val hasMore: Boolean = false
)

/** Error shape the backend sends on 4xx / 5xx responses. */
data class ApiErrorResponse(
    val error: String = "",
    val message: String = "",
    val fieldErrors: Map<String, String> = emptyMap()
)
