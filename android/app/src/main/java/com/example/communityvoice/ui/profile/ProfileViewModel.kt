package com.example.communityvoice.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communityvoice.data.ApiErrorMapper
import com.example.communityvoice.data.FeedSort
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.PublicProfile
import com.example.communityvoice.data.ServiceLocator
import com.example.communityvoice.data.VoteStance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: PublicProfile? = null,
    val issues: List<Issue> = emptyList(),
    val errorMessage: String? = null,

    /** Issues whose vote request is in flight. */
    val pendingVoteIds: Set<String> = emptySet(),

    // Only used when looking at your own profile.
    val isEditing: Boolean = false,
    val editName: String = "",
    val editBio: String = "",
    val editNeighbourhood: String = "",
    val isSavingProfile: Boolean = false,
    val editError: String? = null,

    val userMessage: String? = null
)

/**
 * One member's profile: who they are, what they have contributed, and the
 * issues they raised.
 *
 * The same screen serves "my profile" and "somebody else's", with the edit
 * controls appearing only for your own - which keeps one code path for what is
 * really the same view of a person.
 */
class ProfileViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        const val ARG_USER_ID = "userId"
    }

    private val authRepository = ServiceLocator.authRepository
    private val issueRepository = ServiceLocator.issueRepository

    private val userId: String = savedStateHandle.get<String>(ARG_USER_ID).orEmpty()

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching { authRepository.publicProfile(userId) }
                .onSuccess { profile ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            editName = profile.user.displayName,
                            editBio = profile.user.bio,
                            editNeighbourhood = profile.user.neighbourhood
                        )
                    }
                    loadTheirIssues()
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

    private fun loadTheirIssues() {
        viewModelScope.launch {
            // A failure here is not worth an error screen - the profile header
            // is already on display and still useful on its own.
            runCatching {
                issueRepository.loadIssues(
                    authorId = userId,
                    sort = FeedSort.NEW,
                    page = 0,
                    size = 20
                )
            }.onSuccess { response ->
                _state.update { it.copy(issues = response.items) }
            }
        }
    }

    /** Swaps one row for a fresher copy after a vote on this screen. */
    fun applyUpdatedIssue(updated: Issue) {
        _state.update { current ->
            current.copy(issues = current.issues.map { if (it.id == updated.id) updated else it })
        }
    }

    /**
     * Votes on one of this member's issues without leaving the profile.
     *
     * The same cast-or-withdraw rule as the feed: tapping the side you already
     * chose takes your vote back.
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
            }
                .onSuccess { updated ->
                    _state.update { current ->
                        current.copy(
                            issues = current.issues.map {
                                if (it.id == updated.id) updated else it
                            },
                            pendingVoteIds = current.pendingVoteIds - issue.id
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update { current ->
                        current.copy(
                            pendingVoteIds = current.pendingVoteIds - issue.id,
                            userMessage = ApiErrorMapper.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    // ------------------------------------------------------- editing (self)

    fun startEditing() = _state.update { it.copy(isEditing = true, editError = null) }

    fun cancelEditing() = _state.update { current ->
        val user = current.profile?.user
        current.copy(
            isEditing = false,
            editError = null,
            editName = user?.displayName.orEmpty(),
            editBio = user?.bio.orEmpty(),
            editNeighbourhood = user?.neighbourhood.orEmpty()
        )
    }

    fun onEditNameChanged(value: String) = _state.update {
        it.copy(editName = value, editError = null)
    }

    fun onEditBioChanged(value: String) = _state.update {
        it.copy(editBio = value, editError = null)
    }

    fun onEditNeighbourhoodChanged(value: String) = _state.update {
        it.copy(editNeighbourhood = value, editError = null)
    }

    fun saveProfile() {
        val current = _state.value
        if (current.isSavingProfile) return

        if (current.editName.trim().length < 2) {
            _state.update { it.copy(editError = "That name is too short") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSavingProfile = true, editError = null) }
            runCatching {
                authRepository.updateProfile(
                    displayName = current.editName,
                    bio = current.editBio,
                    neighbourhood = current.editNeighbourhood
                )
            }
                .onSuccess { member ->
                    _state.update { state ->
                        state.copy(
                            isSavingProfile = false,
                            isEditing = false,
                            profile = state.profile?.copy(user = member),
                            userMessage = "Profile saved"
                        )
                    }
                    // The backend rewrites the name on this member's existing
                    // issues, so reload them to show it.
                    loadTheirIssues()
                }
                .onFailure { throwable ->
                    val failure = ApiErrorMapper.toFailure(throwable)
                    _state.update {
                        it.copy(
                            isSavingProfile = false,
                            editError = failure.fieldErrors["displayName"] ?: failure.message
                        )
                    }
                }
        }
    }

    fun onUserMessageShown() = _state.update { it.copy(userMessage = null) }
}
