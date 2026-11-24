package com.communityvoice.backend.service

import com.communityvoice.backend.model.Category
import com.communityvoice.backend.model.Issue
import com.communityvoice.backend.model.IssueStatus
import com.communityvoice.backend.model.StatusUpdate
import com.communityvoice.backend.model.VoteStance
import com.communityvoice.backend.repository.CommentRepository
import com.communityvoice.backend.repository.IssueRepository
import com.communityvoice.backend.security.AuthUser
import com.communityvoice.backend.web.CreateIssueRequest
import com.communityvoice.backend.web.UpdateStatusRequest
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.regex.Pattern
import kotlin.math.min

/** How the feed is ordered. */
enum class IssueSort {
    /** Most supported, ever. */
    TOP,

    /** Newest first. */
    NEW,

    /** Strong recent feeling first - see [Issue.trendingScore]. */
    TRENDING,

    /** Busiest comment threads first. */
    MOST_DISCUSSED,

    /** Where the community is most split - lots of support AND lots of declines. */
    CONTROVERSIAL
}

/** Everything the feed endpoint can be asked for, in one object. */
data class IssueFeedQuery(
    val search: String? = null,
    val category: Category? = null,
    val status: IssueStatus? = null,
    /** Only issues raised by this member. */
    val authorId: String? = null,
    /** Only issues this member has voted on, either way. */
    val votedBy: String? = null,
    val sort: IssueSort = IssueSort.TRENDING,
    val page: Int = 0,
    val size: Int = 20
)

/** One page of the feed, before it is turned into JSON. */
data class IssuePage(
    val items: List<Issue>,
    val page: Int,
    val size: Int,
    val totalItems: Long
)

@Service
class IssueService(
    private val issueRepository: IssueRepository,
    private val commentRepository: CommentRepository,
    private val mongoTemplate: MongoTemplate
) {

    companion object {
        /**
         * TRENDING and CONTROVERSIAL are scored in Kotlin rather than by the
         * database, because both scores change with the clock. To keep that
         * from meaning "load the entire collection", we score the newest
         * candidates only. Anything older than this many issues is ranked by
         * the plain database sort instead - which is the right answer anyway,
         * since a month-old issue is not trending.
         */
        private const val RANKING_WINDOW = 500

        private const val MAX_PAGE_SIZE = 50
    }

    // ------------------------------------------------------------- reading

    fun listIssues(query: IssueFeedQuery): IssuePage {
        val size = query.size.coerceIn(1, MAX_PAGE_SIZE)
        val page = query.page.coerceAtLeast(0)
        val criteria = buildCriteria(query)

        return when (query.sort) {
            IssueSort.TRENDING, IssueSort.CONTROVERSIAL -> rankInMemory(query.sort, criteria, page, size)
            else -> pageFromDatabase(databaseSort(query.sort), criteria, page, size)
        }
    }

    fun getIssue(issueId: String): Issue = loadIssue(issueId)

    // ------------------------------------------------------------- writing

    /**
     * Raises a new issue. The author's name and avatar colour are copied onto
     * the issue so the feed needs one query, and the author counts as its
     * first supporter - you are presumed to back what you raised. They can
     * withdraw that like any other vote.
     */
    fun createIssue(request: CreateIssueRequest, author: AuthUser): Issue {
        val category = checkNotNull(request.category) { "category was validated as non-null" }
        return issueRepository.save(
            Issue(
                title = request.title.trim(),
                description = request.description.trim(),
                category = category,
                location = request.location.trim(),
                authorId = author.id,
                authorName = author.displayName,
                authorAvatarColor = author.avatarColor,
                authorRole = author.role,
                supportCount = 1,
                supporterIds = setOf(author.id)
            )
        )
    }

    /** Only the author, or an admin, may delete an issue. Comments go too. */
    fun deleteIssue(issueId: String, requester: AuthUser) {
        val issue = loadIssue(issueId)
        val isOwner = issue.authorId == requester.id
        if (!isOwner && !requester.role.canDeleteAnything) {
            throw ForbiddenException("Only the person who raised an issue can delete it.")
        }
        commentRepository.deleteByIssueId(issueId)
        issueRepository.deleteById(issueId)
    }

    /**
     * Records [stance] as [voter]'s vote on this issue, whatever they had
     * before. Returns the issue with its new totals.
     *
     * Each of the two cases below is a SINGLE MongoDB update whose query also
     * describes the state it expects to find. That matters: two taps arriving
     * at the same instant cannot both match, so a vote can never be counted
     * twice, and switching sides can never leave both counters raised.
     *
     *   1. No vote yet    - add to my side, +1 my counter.
     *   2. Voted the other way - move across: +1 my counter, -1 the other.
     *   3. Already this stance - nothing to do, hand back what is there.
     */
    fun castVote(issueId: String, voter: AuthUser, stance: VoteStance): Issue {
        val objectId = toObjectId(issueId)
        val mine = if (stance == VoteStance.SUPPORT) "supporterIds" else "declinerIds"
        val theirs = if (stance == VoteStance.SUPPORT) "declinerIds" else "supporterIds"
        val myCount = if (stance == VoteStance.SUPPORT) "supportCount" else "declineCount"
        val theirCount = if (stance == VoteStance.SUPPORT) "declineCount" else "supportCount"

        // Case 1: this person has not voted either way yet.
        findAndModify(
            Query(
                Criteria.where("_id").`is`(objectId)
                    .and("supporterIds").ne(voter.id)
                    .and("declinerIds").ne(voter.id)
            ),
            Update()
                .addToSet(mine, voter.id)
                .inc(myCount, 1)
        )?.let { return it }

        // Case 2: they voted the other way and are changing their mind.
        findAndModify(
            Query(Criteria.where("_id").`is`(objectId).and(theirs).`is`(voter.id)),
            Update()
                .addToSet(mine, voter.id)
                .pull(theirs, voter.id)
                .inc(myCount, 1)
                .inc(theirCount, -1)
        )?.let { return it }

        // Case 3: nothing matched. Either the issue is gone, or this is
        // already their stance - in which case the request was a no-op and
        // returning the current issue is the honest answer.
        return loadIssue(issueId)
    }

    /**
     * Takes [voter]'s vote back off this issue, whichever way it went.
     * Harmless to call when they had not voted.
     */
    fun clearVote(issueId: String, voter: AuthUser): Issue {
        val objectId = toObjectId(issueId)

        findAndModify(
            Query(Criteria.where("_id").`is`(objectId).and("supporterIds").`is`(voter.id)),
            Update().pull("supporterIds", voter.id).inc("supportCount", -1)
        )?.let { return it }

        findAndModify(
            Query(Criteria.where("_id").`is`(objectId).and("declinerIds").`is`(voter.id)),
            Update().pull("declinerIds", voter.id).inc("declineCount", -1)
        )?.let { return it }

        return loadIssue(issueId)
    }

    /**
     * Moves an issue along its lifecycle. Officials and admins only - this is
     * the bit that turns a list of complaints into something with an answer.
     *
     * Every change is appended to the issue's history rather than overwriting
     * it, so the app can show the full story: raised, reviewed, in progress,
     * resolved, with whatever note the official left at each step.
     */
    fun updateStatus(issueId: String, request: UpdateStatusRequest, official: AuthUser): Issue {
        if (!official.role.canModerateStatus) {
            throw ForbiddenException(
                "Only a verified official can change the status of an issue."
            )
        }
        val status = checkNotNull(request.status) { "status was validated as non-null" }
        val issue = loadIssue(issueId)

        val entry = StatusUpdate(
            status = status,
            note = request.note.trim(),
            updatedById = official.id,
            updatedByName = official.displayName
        )

        return issueRepository.save(
            issue.copy(
                status = status,
                updatedAt = Instant.now(),
                statusHistory = issue.statusHistory + entry
            )
        )
    }

    /** Keeps [Issue.commentCount] in step. Called by CommentService. */
    fun adjustCommentCount(issueId: String, delta: Int) {
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(toObjectId(issueId))),
            Update().inc("commentCount", delta),
            Issue::class.java
        )
    }

    // ----------------------------------------------------------- internals

    private fun findAndModify(query: Query, update: Update): Issue? =
        mongoTemplate.findAndModify(
            query,
            update.set("updatedAt", Instant.now()),
            FindAndModifyOptions.options().returnNew(true),
            Issue::class.java
        )

    private fun buildCriteria(query: IssueFeedQuery): Criteria? {
        val parts = mutableListOf<Criteria>()

        query.category?.let { parts += Criteria.where("category").`is`(it) }
        query.status?.let { parts += Criteria.where("status").`is`(it) }
        query.authorId?.takeIf { it.isNotBlank() }
            ?.let { parts += Criteria.where("authorId").`is`(it) }

        query.votedBy?.takeIf { it.isNotBlank() }?.let { voterId ->
            parts += Criteria().orOperator(
                Criteria.where("supporterIds").`is`(voterId),
                Criteria.where("declinerIds").`is`(voterId)
            )
        }

        query.search?.trim()?.takeIf { it.isNotEmpty() }?.let { term ->
            // Pattern.quote wraps the term in \Q...\E so a search for "C++"
            // or "what?" is treated as text, not as regex punctuation.
            val safe = Pattern.quote(term)
            parts += Criteria().orOperator(
                Criteria.where("title").regex(safe, "i"),
                Criteria.where("description").regex(safe, "i"),
                Criteria.where("location").regex(safe, "i"),
                Criteria.where("authorName").regex(safe, "i")
            )
        }

        return if (parts.isEmpty()) null else Criteria().andOperator(*parts.toTypedArray())
    }

    private fun databaseSort(sort: IssueSort): Sort = when (sort) {
        IssueSort.NEW -> Sort.by(Sort.Direction.DESC, "createdAt")
        IssueSort.MOST_DISCUSSED ->
            Sort.by(Sort.Direction.DESC, "commentCount", "supportCount", "createdAt")
        // TOP, and the fallback for the in-memory sorts.
        else -> Sort.by(Sort.Direction.DESC, "supportCount", "createdAt")
    }

    private fun pageFromDatabase(sort: Sort, criteria: Criteria?, page: Int, size: Int): IssuePage {
        val query = Query().with(sort).skip((page.toLong() * size)).limit(size)
        val countQuery = Query()
        criteria?.let {
            query.addCriteria(it)
            countQuery.addCriteria(it)
        }
        return IssuePage(
            items = findIssues(query),
            page = page,
            size = size,
            totalItems = mongoTemplate.count(countQuery, Issue::class.java)
        )
    }

    private fun rankInMemory(
        sort: IssueSort,
        criteria: Criteria?,
        page: Int,
        size: Int
    ): IssuePage {
        val candidateQuery = Query()
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .limit(RANKING_WINDOW)
        criteria?.let { candidateQuery.addCriteria(it) }

        val now = Instant.now()
        val candidates = findIssues(candidateQuery)
        val ranked = when (sort) {
            IssueSort.CONTROVERSIAL -> candidates
                .filter { it.supportCount + it.declineCount >= 2 }
                .sortedByDescending { controversyScore(it) }
            // TRENDING
            else -> candidates.sortedByDescending { it.trendingScore(now) }
        }

        val from = min(page.toLong() * size, ranked.size.toLong()).toInt()
        val to = min(from + size, ranked.size)
        return IssuePage(
            items = if (from >= to) emptyList() else ranked.subList(from, to),
            page = page,
            size = size,
            // Count the ranked set, not the whole collection, so "no more
            // pages" lines up with what this ordering can actually return.
            totalItems = ranked.size.toLong()
        )
    }

    /**
     * How split the community is. Busy *and* balanced scores highest:
     * 50 support / 50 decline beats 100 / 1, and 2 / 1 beats neither.
     */
    private fun controversyScore(issue: Issue): Double {
        val support = issue.supportCount.toDouble()
        val decline = issue.declineCount.toDouble()
        val total = support + decline
        if (total < 2) return 0.0
        val balance = min(support, decline) / maxOf(support, decline).coerceAtLeast(1.0)
        return total * balance
    }

    private fun findIssues(query: Query): List<Issue> =
        mongoTemplate.find(query, Document::class.java, "issues").map { stored ->
            // Older demo records can contain explicit nulls, which bypass
            // Kotlin constructor defaults and otherwise fail the whole feed.
            val document = Document(stored)
            document.defaultIfNull("status", IssueStatus.OPEN.name)
            document.defaultIfNull("location", "")
            document.defaultIfNull("authorId", "")
            document.defaultIfNull("authorName", "Community member")
            document.defaultIfNull("authorAvatarColor", "#1B6B4A")
            document.defaultIfNull("authorRole", "CITIZEN")
            document.defaultIfNull("supportCount", 0)
            document.defaultIfNull("declineCount", 0)
            document.defaultIfNull("supporterIds", emptyList<String>())
            document.defaultIfNull("declinerIds", emptyList<String>())
            document.defaultIfNull("commentCount", 0)
            document.defaultIfNull("createdAt", Date())
            document.defaultIfNull("updatedAt", checkNotNull(document["createdAt"]))
            document.defaultIfNull("statusHistory", emptyList<Document>())
            mongoTemplate.converter.read(Issue::class.java, document)
        }

    private fun Document.defaultIfNull(field: String, fallback: Any) {
        if (this[field] == null) this[field] = fallback
    }

    private fun loadIssue(issueId: String): Issue {
        if (!ObjectId.isValid(issueId)) throw IssueNotFoundException(issueId)
        return issueRepository.findById(issueId)
            .orElseThrow { IssueNotFoundException(issueId) }
    }

    private fun toObjectId(issueId: String): ObjectId {
        if (!ObjectId.isValid(issueId)) throw IssueNotFoundException(issueId)
        return ObjectId(issueId)
    }
}
