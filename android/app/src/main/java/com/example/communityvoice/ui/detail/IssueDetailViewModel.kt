package com.example.communityvoice.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communityvoice.data.ApiErrorMapper
import com.example.communityvoice.data.Comment
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.IssueStatus
import com.example.communityvoice.data.ServiceLocator
import com.example.communityvoice.data.VoteStance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IssueDetailUiState(
    val isLoading: Boolean = true,
    val issue: Issue? = null,
    val errorMessage: String? = null,

    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val commentsError: String? = null,
    val hasMoreComments: Boolean = false,
    val commentPage: Int = 0,

    val commentDraft: String = "",
    val isPostingComment: Boolean = false,

    val isVoting: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val isDeleting: Boolean = false,

    /** Set once the issue has been deleted, so the screen can close itself. */
    val wasDeleted: Boolean = false,
    val userMessage: String? = null
) {
    val canPostComment: Boolean
        get() = commentDraft.isNotBlank() && !isPostingComment
}

/**
 * Drives one issue's detail screen: the issue itself, its official history,
 * its discussion, and the actions available on it.
 *
 * The issue id arrives through [SavedStateHandle], which Navigation fills in
 * from the `issue/{issueId}` route. That also means it survives the process
 * being killed in the background.
 */
class IssueDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        /** The name of the path segment in the navigation route. */
        const val ARG_ISSUE_ID = "issueId"

        private const val COMMENT_PAGE_SIZE = 30
    }

    // Taken from ServiceLocator inside the class rather than as constructor
    // defaults: Navigation's default ViewModel factory looks for a
    // constructor taking exactly a SavedStateHandle.
    private val issueRepository = ServiceLocator.issueRepository
    private val commentRepository = ServiceLocator.commentRepository

    private val issueId: String = savedStateHandle.get<String>(ARG_ISSUE_ID).orEmpty()

    private val _state = MutableStateFlow(IssueDetailUiState())
    val state: StateFlow<IssueDetailUiState> = _state.asStateFlow()

    /**
     * The newest copy of the issue, for the feed to pick up when the user goes
     * back - so the card they came from shows the vote they just cast.
     */
    val latestIssue: Issue? get() = _state.value.issue

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { issueRepository.loadIssue(issueId) }
                .onSuccess { issue ->
                    _state.update { it.copy(isLoading = false, issue = issue) }
                    loadComments(reset = true)
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ApiErrorMapper.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    // ------------------------------------------------------------- comments

    fun loadComments(reset: Boolean = false) {
        val current = _state.value
        if (current.isLoadingComments) return
        val page = if (reset) 0 else current.commentPage + 1
        if (!reset && !current.hasMoreComments) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingComments = true, commentsError = null) }
            runCatching { commentRepository.loadComments(issueId, page, COMMENT_PAGE_SIZE) }
                .onSuccess { response ->
                    _state.update { state ->
                        state.copy(
                            isLoadingComments = false,
                            comments = if (reset) {
                                response.items
                            } else {
                                state.comments + response.items
                            },
                            commentPage = response.page,
                            hasMoreComments = response.hasMore
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoadingComments = false,
                            commentsError = ApiErrorMapper.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    fun onCommentDraftChanged(value: String) = _state.update { it.copy(commentDraft = value) }

    fun postComment() {
        val current = _state.value
        if (!current.canPostComment) return
        val body = current.commentDraft

        viewModelScope.launch {
            _state.update { it.copy(isPostingComment = true) }
            runCatching { commentRepository.addComment(issueId, body) }
                .onSuccess { comment ->
                    _state.update { state ->
                        state.copy(
                            isPostingComment = false,
                            commentDraft = "",
                            // Comments read oldest first, so a new one goes
                            // on the end, where the thread was heading.
                            comments = state.comments + comment,
                            issue = state.issue?.let { it.copy(commentCount = it.commentCount + 1) }
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isPostingComment = false,
                            userMessage = ApiErrorMapper.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            runCatching { commentRepository.deleteComment(comment.id) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            comments = state.comments.filterNot { it.id == comment.id },
                            issue = state.issue?.let {
                                it.copy(commentCount = (it.commentCount - 1).coerceAtLeast(0))
                            },
                            userMessage = "Comment removed"
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(userMessage = ApiErrorMapper.toUserMessage(throwable))
                    }
                }
        }
    }

    // --------------------------------------------------------------- voting

    /** Tapping your current stance withdraws it; the other side moves it. */
    fun vote(stance: VoteStance) {
        val issue = _state.value.issue ?: return
        if (_state.value.isVoting) return
        val withdrawing = issue.stance == stance

        viewModelScope.launch {
            _state.update { it.copy(isVoting = true) }
            runCatching {
                if (withdrawing) {
                    issueRepository.clearVote(issue.id)
                } else {
                    issueRepository.vote(issue.id, stance)
                }
            }
                .onSuccess { updated ->
                    _state.update {
                        it.copy(
                            isVoting = false,
                            issue = updated,
                            userMessage = when {
                                withdrawing -> "Vote withdrawn"
                                stance == VoteStance.SUPPORT -> "Thanks for your support"
                                else -> "Your objection has been recorded"
                            }
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isVoting = false,
                            userMessage = ApiErrorMapper.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    // ----------------------------------------------------- official actions

    /** Officials only - the backend rejects this from anybody else. */
    fun updateStatus(status: IssueStatus, note: String) {
        val issue = _state.value.issue ?: return

        viewModelScope.launch {
            _state.update { it.copy(isUpdatingStatus = true) }
            runCatching { issueRepository.updateStatus(issue.id, status, note) }
                .onSuccess { updated ->
                    _state.update {
                        it.copy(
                            isUpdatingStatus = false,
                            issue = updated,
                            userMessage = "Status set to ${status.label}"
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isUpdatingStatus = false,
                            userMessage = ApiErrorMapper.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    fun deleteIssue() {
        val issue = _state.value.issue ?: return

        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            runCatching { issueRepository.deleteIssue(issue.id) }
                .onSuccess { _state.update { it.copy(isDeleting = false, wasDeleted = true) } }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            userMessage = ApiErrorMapper.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    fun onUserMessageShown() = _state.update { it.copy(userMessage = null) }
}
