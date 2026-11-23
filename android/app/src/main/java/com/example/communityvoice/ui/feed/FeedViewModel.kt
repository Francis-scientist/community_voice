package com.example.communityvoice.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communityvoice.data.ApiErrorMapper
import com.example.communityvoice.data.FeedSort
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.IssueCategory
import com.example.communityvoice.data.IssueRepository
import com.example.communityvoice.data.IssueStatus
import com.example.communityvoice.data.ServiceLocator
import com.example.communityvoice.data.VoteStance
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which slice of the feed is on screen. */
enum class FeedScope(val label: String) {
    EVERYTHING("All issues"),
    MINE("Raised by me"),
    VOTED("I voted on")
}

data class FeedUiState(
    /** First load, or a load after the filters changed. */
    val isLoading: Boolean = false,
    /** A pull-to-refresh on an already-populated list. */
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val issues: List<Issue> = emptyList(),
    val errorMessage: String? = null,

    // filters
    val search: String = "",
    val category: IssueCategory? = null,
    val status: IssueStatus? = null,
    val sort: FeedSort = FeedSort.TRENDING,
    val scope: FeedScope = FeedScope.EVERYTHING,
    val filtersExpanded: Boolean = false,

    // paging
    val page: Int = 0,
    val hasMore: Boolean = false,
    val totalItems: Long = 0,

    /** Issues whose vote request is in flight, so the buttons can disable. */
    val pendingVoteIds: Set<String> = emptySet(),
    /** One-off text for a snackbar, then cleared. */
    val userMessage: String? = null
) {
    /** True only when a successful load came back with nothing. */
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && issues.isEmpty()

    /** Any filter narrowing the feed, which the "Clear" button reacts to. */
    val hasActiveFilters: Boolean
        get() = search.isNotBlank() ||
            category != null ||
            status != null ||
            scope != FeedScope.EVERYTHING ||
            sort != FeedSort.TRENDING
}

/**
 * Drives the feed: filtering, searching, paging and voting.
 *
 * This ViewModel is created once at the top of the navigation graph rather
 * than per screen, so the other screens can ask it to refresh after they
 * change something - posting an issue, or voting from the detail screen.
 */
class FeedViewModel(
    private val issueRepository: IssueRepository = ServiceLocator.issueRepository
) : ViewModel() {

    private companion object {
        const val PAGE_SIZE = 20

        /**
         * How long to wait after the last keystroke before searching. Short
         * enough to feel instant, long enough that typing "water" is one
         * request rather than five.
         */
        const val SEARCH_DEBOUNCE_MS = 350L
    }

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    /** Cancelled and replaced on every keystroke - see [onSearchChanged]. */
    private var searchJob: Job? = null

    init {
        load()
    }

    // ------------------------------------------------------------- loading

    /** A fresh load of page 0 with the current filters. */
    fun load() {
        fetchPage(page = 0, mode = LoadMode.INITIAL)
    }

    /** Pull-to-refresh: reload page 0 but leave the list on screen. */
    fun refresh() {
        fetchPage(page = 0, mode = LoadMode.REFRESH)
    }

    /** Called when the list is scrolled near the bottom. */
    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.isLoadingMore || current.isLoading) return
        fetchPage(page = current.page + 1, mode = LoadMode.APPEND)
    }

    private enum class LoadMode { INITIAL, REFRESH, APPEND }

    private fun fetchPage(page: Int, mode: LoadMode) {
        viewModelScope.launch {
            _state.update {
                when (mode) {
                    LoadMode.INITIAL -> it.copy(isLoading = true, errorMessage = null)
                    LoadMode.REFRESH -> it.copy(isRefreshing = true, errorMessage = null)
                    LoadMode.APPEND -> it.copy(isLoadingMore = true)
                }
            }

            val filters = _state.value
            runCatching {
                issueRepository.loadIssues(
                    search = filters.search,
                    category = filters.category,
                    status = filters.status,
                    authorId = if (filters.scope == FeedScope.MINE) "me" else null,
                    votedBy = if (filters.scope == FeedScope.VOTED) "me" else null,
                    sort = filters.sort,
                    page = page,
                    size = PAGE_SIZE
                )
            }.onSuccess { response ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        issues = if (mode == LoadMode.APPEND) {
                            // Guard against a duplicate row if an issue moved
                            // between pages while we were paging.
                            val seen = current.issues.mapTo(mutableSetOf()) { it.id }
                            current.issues + response.items.filter { it.id !in seen }
                        } else {
                            response.items
                        },
                        page = response.page,
                        hasMore = response.hasMore,
                        totalItems = response.totalItems
                    )
                }
            }.onFailure { throwable ->
                val message = ApiErrorMapper.toUserMessage(throwable)
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        // Keep what is on screen if we already had rows: a
                        // failed "load more" should not blank the feed.
                        errorMessage = if (current.issues.isEmpty()) message else null,
                        userMessage = if (current.issues.isEmpty()) null else message
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------- filters

    fun onSearchChanged(value: String) {
        _state.update { it.copy(search = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load()
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update { it.copy(search = "") }
        load()
    }

    /** Tapping the selected category again clears it. */
    fun onCategorySelected(category: IssueCategory?) {
        _state.update { it.copy(category = if (it.category == category) null else category) }
        load()
    }

    fun onStatusSelected(status: IssueStatus?) {
        _state.update { it.copy(status = if (it.status == status) null else status) }
        load()
    }

    fun onSortSelected(sort: FeedSort) {
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort) }
        load()
    }

    fun onScopeSelected(scope: FeedScope) {
        if (_state.value.scope == scope) return
        _state.update { it.copy(scope = scope) }
        load()
    }

    fun toggleFilters() = _state.update { it.copy(filtersExpanded = !it.filtersExpanded) }

    fun clearFilters() {
        searchJob?.cancel()
        _state.update {
            it.copy(
                search = "",
                category = null,
                status = null,
                sort = FeedSort.TRENDING,
                scope = FeedScope.EVERYTHING
            )
        }
        load()
    }

    // --------------------------------------------------------------- voting

    /**
     * Records a vote, or withdraws it.
     *
     * Tapping the side you already picked takes the vote back - the same
     * button both casts and cancels, which is what people expect from a
     * thumbs-up. Tapping the other side moves the single vote across.
     */
    fun vote(issue: Issue, stance: VoteStance) {
        if (issue.id in _state.value.pendingVoteIds) return
        val withdrawing = issue.stance == stance

        viewModelScope.launch {
            _state.update { it.copy(pendingVoteIds = it.pendingVoteIds + issue.id) }

            runCatching {
                if (withdrawing) {
                    issueRepository.clearVote(issue.id)
                } else {
                    issueRepository.vote(issue.id, stance)
                }
            }.onSuccess { updated ->
                _state.update { current ->
                    current.copy(
                        issues = current.issues.map { if (it.id == updated.id) updated else it },
                        pendingVoteIds = current.pendingVoteIds - issue.id,
                        userMessage = when {
                            withdrawing -> "Vote withdrawn"
                            stance == VoteStance.SUPPORT -> "Thanks for your support"
                            else -> "Your objection has been recorded"
                        }
                    )
                }
            }.onFailure { throwable ->
                _state.update { current ->
                    current.copy(
                        pendingVoteIds = current.pendingVoteIds - issue.id,
                        userMessage = ApiErrorMapper.toUserMessage(throwable)
                    )
                }
            }
        }
    }

    /**
     * Replaces one row with a newer copy - used when the detail screen votes
     * or an official changes a status, so going back shows the new numbers
     * without a round trip.
     */
    fun applyUpdatedIssue(updated: Issue) {
        _state.update { current ->
            current.copy(issues = current.issues.map { if (it.id == updated.id) updated else it })
        }
    }

    /** Drops a deleted issue from the list immediately. */
    fun removeIssue(issueId: String) {
        _state.update { current ->
            current.copy(
                issues = current.issues.filterNot { it.id == issueId },
                totalItems = (current.totalItems - 1).coerceAtLeast(0)
            )
        }
    }

    fun showMessage(message: String) = _state.update { it.copy(userMessage = message) }

    /** Call after a snackbar has been shown so it is not shown twice. */
    fun onUserMessageShown() = _state.update { it.copy(userMessage = null) }
}
