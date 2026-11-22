package com.communityvoice.backend.web

import com.communityvoice.backend.model.Category
import com.communityvoice.backend.model.IssueStatus
import com.communityvoice.backend.security.AuthUser
import com.communityvoice.backend.security.CurrentUser
import com.communityvoice.backend.service.InvalidRequestException
import com.communityvoice.backend.service.IssueFeedQuery
import com.communityvoice.backend.service.IssueService
import com.communityvoice.backend.service.IssueSort
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/issues")
class IssueController(private val issueService: IssueService) {

    /**
     * GET /api/issues - the feed.
     *
     * Everything is optional:
     *
     *   q=pump             free text over title, description, location, author
     *   category=WATER     one category
     *   status=IN_PROGRESS one status
     *   author=<userId>    only this member's issues ("mine" means your own)
     *   votedBy=me         only issues you have voted on
     *   sort=trending      trending | top | new | discussed | controversial
     *   page=0&size=20     paging
     *
     * A token is optional. Send one and each issue comes back with your own
     * vote in `myVote`; leave it off and the counts are still there, which is
     * what lets somebody browse before deciding to sign up.
     */
    @GetMapping
    fun listIssues(
        @CurrentUser(required = false) viewer: AuthUser?,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) votedBy: String?,
        @RequestParam(required = false, defaultValue = "trending") sort: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): PageResponse<IssueResponse> {
        val result = issueService.listIssues(
            IssueFeedQuery(
                search = q,
                category = parseCategory(category),
                status = parseStatus(status),
                authorId = resolveMemberId(author, viewer, "author"),
                votedBy = resolveMemberId(votedBy, viewer, "votedBy"),
                sort = parseSort(sort),
                page = page,
                size = size
            )
        )
        return PageResponse.of(
            items = result.items.map { IssueResponse.from(it, viewer) },
            page = result.page,
            size = result.size,
            totalItems = result.totalItems
        )
    }

    /** GET /api/issues/{id} - one issue, with its full status history. */
    @GetMapping("/{id}")
    fun getIssue(
        @CurrentUser(required = false) viewer: AuthUser?,
        @PathVariable id: String
    ): IssueResponse = IssueResponse.from(issueService.getIssue(id), viewer)

    /** POST /api/issues - raise an issue. You must be signed in. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createIssue(
        @CurrentUser author: AuthUser,
        @Valid @RequestBody request: CreateIssueRequest
    ): IssueResponse = IssueResponse.from(issueService.createIssue(request, author), author)

    /** DELETE /api/issues/{id} - the author or an admin only. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteIssue(@CurrentUser requester: AuthUser, @PathVariable id: String) {
        issueService.deleteIssue(id, requester)
    }

    /**
     * PUT /api/issues/{id}/vote - support or decline.
     *
     * PUT rather than POST because it *sets* your vote: sending SUPPORT when
     * you had already supported changes nothing, and sending DECLINE after
     * SUPPORT moves your single vote across instead of adding a second one.
     */
    @PutMapping("/{id}/vote")
    fun vote(
        @CurrentUser voter: AuthUser,
        @PathVariable id: String,
        @Valid @RequestBody request: VoteRequest
    ): IssueResponse {
        val stance = checkNotNull(request.stance) { "stance was validated as non-null" }
        return IssueResponse.from(issueService.castVote(id, voter, stance), voter)
    }

    /** DELETE /api/issues/{id}/vote - take your vote back. */
    @DeleteMapping("/{id}/vote")
    fun clearVote(
        @CurrentUser voter: AuthUser,
        @PathVariable id: String
    ): IssueResponse = IssueResponse.from(issueService.clearVote(id, voter), voter)

    /** PATCH /api/issues/{id}/status - officials and admins only. */
    @PatchMapping("/{id}/status")
    fun updateStatus(
        @CurrentUser official: AuthUser,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateStatusRequest
    ): IssueResponse =
        IssueResponse.from(issueService.updateStatus(id, request, official), official)

    // ------------------------------------------------- lenient param parsing
    // Query strings are typed by hand, so "water" and "WATER" both work and a
    // typo gets a 400 listing the valid values rather than a 500.

    private fun parseCategory(raw: String?): Category? =
        parseEnum(raw, "category", Category.entries)

    private fun parseStatus(raw: String?): IssueStatus? =
        parseEnum(raw, "status", IssueStatus.entries)

    private fun parseSort(raw: String): IssueSort = when (raw.trim().lowercase()) {
        "", "trending" -> IssueSort.TRENDING
        "top", "supported" -> IssueSort.TOP
        "new", "newest", "recent" -> IssueSort.NEW
        "discussed", "most_discussed", "comments" -> IssueSort.MOST_DISCUSSED
        "controversial", "split" -> IssueSort.CONTROVERSIAL
        else -> throw InvalidRequestException(
            "sort must be one of trending, top, new, discussed, controversial"
        )
    }

    private fun <E : Enum<E>> parseEnum(raw: String?, name: String, values: List<E>): E? {
        val wanted = raw?.trim()?.takeIf { it.isNotEmpty() && !it.equals("all", true) }
            ?: return null
        return values.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
            ?: throw InvalidRequestException(
                "$name must be one of ${values.joinToString(", ") { it.name }}"
            )
    }

    /**
     * Turns the literal "me" into the caller's own id, so the app can ask for
     * `author=me` without first having to know its own user id.
     */
    private fun resolveMemberId(raw: String?, viewer: AuthUser?, name: String): String? {
        val wanted = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!wanted.equals("me", ignoreCase = true)) return wanted
        return viewer?.id
            ?: throw InvalidRequestException("Sign in to use $name=me")
    }
}
