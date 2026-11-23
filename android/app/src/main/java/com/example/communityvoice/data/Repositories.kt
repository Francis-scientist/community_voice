package com.example.communityvoice.data

/**
 * The single place the rest of the app goes to for issue data.
 *
 * Keeping the network calls behind these classes means the ViewModels do not
 * need to know anything about Retrofit.
 */
class IssueRepository(private val api: CommunityVoiceApi) {

    /** One page of the feed. Every argument is optional. */
    suspend fun loadIssues(
        search: String? = null,
        category: IssueCategory? = null,
        status: IssueStatus? = null,
        authorId: String? = null,
        votedBy: String? = null,
        sort: FeedSort = FeedSort.TRENDING,
        page: Int = 0,
        size: Int = 20
    ): PagedResponse<Issue> = api.getIssues(
        search = search?.trim()?.takeIf { it.isNotEmpty() },
        category = category?.apiValue,
        status = status?.apiValue,
        author = authorId,
        votedBy = votedBy,
        sort = sort.apiValue,
        page = page,
        size = size
    )

    suspend fun loadIssue(issueId: String): Issue = api.getIssue(issueId)

    suspend fun createIssue(
        title: String,
        description: String,
        category: IssueCategory,
        location: String
    ): Issue = api.createIssue(
        CreateIssueRequest(
            title = title.trim(),
            description = description.trim(),
            category = category.apiValue,
            location = location.trim()
        )
    )

    suspend fun deleteIssue(issueId: String) = api.deleteIssue(issueId)

    /** Sets your vote. Sending the opposite stance moves it, not doubles it. */
    suspend fun vote(issueId: String, stance: VoteStance): Issue =
        api.vote(issueId, VoteRequest(stance = stance.apiValue))

    /** Takes your vote back off, whichever way it went. */
    suspend fun clearVote(issueId: String): Issue = api.clearVote(issueId)

    suspend fun updateStatus(issueId: String, status: IssueStatus, note: String): Issue =
        api.updateStatus(issueId, UpdateStatusRequest(status.apiValue, note.trim()))
}

/** How the feed is ordered. [label] is what the chip row shows. */
enum class FeedSort(val apiValue: String, val label: String) {
    TRENDING("trending", "Trending"),
    TOP("top", "Most supported"),
    NEW("new", "Newest"),
    MOST_DISCUSSED("discussed", "Most discussed"),
    CONTROVERSIAL("controversial", "Most debated")
}

class CommentRepository(private val api: CommunityVoiceApi) {

    suspend fun loadComments(issueId: String, page: Int = 0, size: Int = 30):
        PagedResponse<Comment> = api.getComments(issueId, page, size)

    suspend fun addComment(issueId: String, body: String): Comment =
        api.addComment(issueId, CreateCommentRequest(body.trim()))

    suspend fun deleteComment(commentId: String) = api.deleteComment(commentId)
}

class StatsRepository(private val api: CommunityVoiceApi) {
    suspend fun loadStats(): CommunityStats = api.getStats()
}

/**
 * Registering, signing in and profiles.
 *
 * Note that this is the only repository that touches [SessionStore]: it saves
 * the token on the way back from a successful sign-in, so no screen has to
 * remember to do that.
 */
class AuthRepository(
    private val api: CommunityVoiceApi,
    private val sessionStore: SessionStore
) {

    suspend fun register(
        displayName: String,
        email: String,
        password: String,
        neighbourhood: String
    ): Member {
        val response = api.register(
            RegisterRequest(
                displayName = displayName.trim(),
                email = email.trim(),
                password = password,
                neighbourhood = neighbourhood.trim()
            )
        )
        sessionStore.signIn(response)
        return response.user
    }

    suspend fun login(email: String, password: String): Member {
        val response = api.login(LoginRequest(email = email.trim(), password = password))
        sessionStore.signIn(response)
        return response.user
    }

    /**
     * Checks the stored token at startup and refreshes the cached profile.
     * A 401 here makes ApiClient sign the user out, so the caller only needs
     * to catch and ignore the failure.
     */
    suspend fun refreshCurrentMember(): Member {
        val member = api.me()
        sessionStore.updateMember(member)
        return member
    }

    suspend fun updateProfile(
        displayName: String?,
        bio: String?,
        neighbourhood: String?
    ): Member {
        val member = api.updateProfile(
            UpdateProfileRequest(
                displayName = displayName?.trim(),
                bio = bio?.trim(),
                neighbourhood = neighbourhood?.trim()
            )
        )
        sessionStore.updateMember(member)
        return member
    }

    suspend fun publicProfile(userId: String): PublicProfile = api.publicProfile(userId)

    fun signOut() = sessionStore.signOut()
}
