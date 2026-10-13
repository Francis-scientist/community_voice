package com.example.communityvoice.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communityvoice.data.ApiErrorMapper
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.IssueCategory
import com.example.communityvoice.data.IssueRepository
import com.example.communityvoice.data.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The limits the backend enforces, repeated here so the form can show a
 * character counter and refuse to submit something it knows will be rejected.
 * If these ever disagree, the backend wins - it is the only side that matters.
 */
object IssueLimits {
    const val TITLE_MIN = 6
    const val TITLE_MAX = 120
    const val DESCRIPTION_MIN = 20
    const val DESCRIPTION_MAX = 2000
    const val LOCATION_MAX = 120
}

data class NewIssueFormState(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val category: IssueCategory? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val showPreview: Boolean = false
) {
    /** Enough has been filled in that a preview would show something real. */
    val canPreview: Boolean
        get() = title.isNotBlank() && description.isNotBlank() && category != null

    val titleCharactersLeft: Int get() = IssueLimits.TITLE_MAX - title.length
    val descriptionCharactersLeft: Int get() = IssueLimits.DESCRIPTION_MAX - description.length
}

class CreateIssueViewModel(
    private val issueRepository: IssueRepository = ServiceLocator.issueRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NewIssueFormState())
    val state: StateFlow<NewIssueFormState> = _state.asStateFlow()

    fun onTitleChanged(value: String) {
        if (value.length > IssueLimits.TITLE_MAX) return
        _state.update { it.copy(title = value, titleError = null, submitError = null) }
    }

    fun onDescriptionChanged(value: String) {
        if (value.length > IssueLimits.DESCRIPTION_MAX) return
        _state.update { it.copy(description = value, descriptionError = null, submitError = null) }
    }

    fun onLocationChanged(value: String) {
        if (value.length > IssueLimits.LOCATION_MAX) return
        _state.update { it.copy(location = value, submitError = null) }
    }

    fun onCategorySelected(category: IssueCategory) {
        _state.update { it.copy(category = category, categoryError = null, submitError = null) }
    }

    fun togglePreview() = _state.update { it.copy(showPreview = !it.showPreview) }

    fun reset() {
        _state.value = NewIssueFormState()
    }

    /**
     * Checks the fields, then posts. [onSuccess] receives the created issue so
     * the caller can slot it straight into the feed and open it.
     */
    fun submit(onSuccess: (Issue) -> Unit) {
        val current = _state.value
        if (current.isSubmitting) return

        val titleError = when {
            current.title.isBlank() -> "Please enter a title"
            current.title.trim().length < IssueLimits.TITLE_MIN ->
                "A bit more detail - at least ${IssueLimits.TITLE_MIN} characters"
            else -> null
        }
        val descriptionError = when {
            current.description.isBlank() -> "Please describe the issue"
            current.description.trim().length < IssueLimits.DESCRIPTION_MIN ->
                "Say a little more - at least ${IssueLimits.DESCRIPTION_MIN} characters"
            else -> null
        }
        val categoryError = if (current.category == null) "Please choose a category" else null

        _state.update {
            it.copy(
                titleError = titleError,
                descriptionError = descriptionError,
                categoryError = categoryError
            )
        }

        val category = current.category
        if (titleError != null || descriptionError != null || category == null) return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, submitError = null) }
            runCatching {
                issueRepository.createIssue(
                    title = current.title,
                    description = current.description,
                    category = category,
                    location = current.location
                )
            }
                .onSuccess { created ->
                    reset()
                    onSuccess(created)
                }
                .onFailure { throwable ->
                    val failure = ApiErrorMapper.toFailure(throwable)
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            titleError = failure.fieldErrors["title"],
                            descriptionError = failure.fieldErrors["description"],
                            categoryError = failure.fieldErrors["category"],
                            submitError = if (failure.fieldErrors.isEmpty()) {
                                failure.message
                            } else {
                                null
                            }
                        )
                    }
                }
        }
    }
}
