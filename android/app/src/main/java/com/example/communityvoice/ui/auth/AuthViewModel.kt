package com.example.communityvoice.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.communityvoice.data.ApiErrorMapper
import com.example.communityvoice.data.AuthRepository
import com.example.communityvoice.data.Member
import com.example.communityvoice.data.ServiceLocator
import com.example.communityvoice.data.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * How strong a typed password looks, shown as a little meter under the box.
 *
 * This is a nudge, not a gate - the backend's rule (eight characters with at
 * least one letter and one number) is the only thing actually enforced, and it
 * is enforced there rather than here so it cannot be bypassed.
 */
enum class PasswordStrength(val label: String, val fraction: Float) {
    TOO_SHORT("Too short", 0.15f),
    WEAK("Weak", 0.35f),
    FAIR("Fair", 0.6f),
    STRONG("Strong", 1f);

    companion object {
        fun of(password: String): PasswordStrength {
            if (password.length < 8) return TOO_SHORT

            var variety = 0
            if (password.any { it.isDigit() }) variety++
            if (password.any { it.isLowerCase() }) variety++
            if (password.any { it.isUpperCase() }) variety++
            if (password.any { !it.isLetterOrDigit() }) variety++

            return when {
                password.length >= 12 && variety >= 3 -> STRONG
                variety >= 2 -> FAIR
                else -> WEAK
            }
        }
    }
}

/** State shared by the sign-in and sign-up forms. */
data class AuthFormState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val neighbourhood: String = "",
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val passwordVisible: Boolean = false,
    /** A problem that belongs to the whole form, not one field. */
    val formError: String? = null
) {
    val passwordStrength: PasswordStrength get() = PasswordStrength.of(password)
}

/**
 * Drives both the sign-in and the sign-up form.
 *
 * Fields are validated here for a fast, friendly response, and then again by
 * the backend, which has the final say. When the server rejects a specific
 * field it sends `fieldErrors`, and [applyFailure] puts each message under the
 * right box rather than dumping one sentence at the bottom.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    fun onDisplayNameChanged(value: String) = _state.update {
        it.copy(displayName = value, displayNameError = null, formError = null)
    }

    fun onEmailChanged(value: String) = _state.update {
        it.copy(email = value, emailError = null, formError = null)
    }

    fun onPasswordChanged(value: String) = _state.update {
        it.copy(password = value, passwordError = null, formError = null)
    }

    fun onNeighbourhoodChanged(value: String) = _state.update {
        it.copy(neighbourhood = value, formError = null)
    }

    fun togglePasswordVisible() = _state.update {
        it.copy(passwordVisible = !it.passwordVisible)
    }

    /** Called when moving between the sign-in and sign-up screens. */
    fun reset() {
        _state.value = AuthFormState()
    }

    fun login(onSuccess: (Member) -> Unit) {
        val current = _state.value
        if (current.isSubmitting) return

        val emailError = validateEmail(current.email)
        val passwordError = if (current.password.isEmpty()) "Enter your password" else null
        _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
        if (emailError != null || passwordError != null) return

        submit(onSuccess) { authRepository.login(current.email, current.password) }
    }

    fun register(onSuccess: (Member) -> Unit) {
        val current = _state.value
        if (current.isSubmitting) return

        val nameError = when {
            current.displayName.isBlank() -> "Enter your name"
            current.displayName.trim().length < 2 -> "That name is too short"
            else -> null
        }
        val emailError = validateEmail(current.email)
        val passwordError = when {
            current.password.length < 8 -> "Use at least 8 characters"
            !current.password.any { it.isDigit() } -> "Include at least one number"
            !current.password.any { it.isLetter() } -> "Include at least one letter"
            else -> null
        }

        _state.update {
            it.copy(
                displayNameError = nameError,
                emailError = emailError,
                passwordError = passwordError
            )
        }
        if (nameError != null || emailError != null || passwordError != null) return

        submit(onSuccess) {
            authRepository.register(
                displayName = current.displayName,
                email = current.email,
                password = current.password,
                neighbourhood = current.neighbourhood
            )
        }
    }

    private fun submit(onSuccess: (Member) -> Unit, call: suspend () -> Member) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, formError = null) }
            runCatching { call() }
                .onSuccess { member ->
                    // Clear the form before leaving, so the typed password is
                    // not sitting in memory behind the next screen.
                    _state.value = AuthFormState()
                    onSuccess(member)
                }
                .onFailure { throwable -> applyFailure(throwable) }
        }
    }

    private fun applyFailure(throwable: Throwable) {
        val failure = ApiErrorMapper.toFailure(throwable)
        _state.update { current ->
            current.copy(
                isSubmitting = false,
                displayNameError = failure.fieldErrors["displayName"] ?: current.displayNameError,
                emailError = failure.fieldErrors["email"],
                passwordError = failure.fieldErrors["password"],
                // Only show the banner when no single field owns the problem,
                // so the message is never duplicated on screen.
                formError = if (failure.fieldErrors.keys.any {
                        it in setOf("displayName", "email", "password")
                    }
                ) {
                    null
                } else {
                    failure.message
                }
            )
        }
    }

    private fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "Enter your email"
            // Deliberately loose: the backend does the real check, and an
            // over-strict pattern here would reject valid addresses.
            !trimmed.contains("@") || !trimmed.substringAfterLast("@").contains(".") ->
                "That does not look like an email address"
            else -> null
        }
    }
}

/**
 * Holds the app-wide answer to "who is signed in?".
 *
 * On startup it calls GET /api/me to confirm the saved token still works. If
 * the token has expired, ApiClient clears the session, the flow below emits
 * null, and the navigation graph shows the welcome screen - without the user
 * ever seeing an error.
 */
class SessionViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    val session: StateFlow<Session?> = ServiceLocator.sessionStore.session

    private val _isCheckingSession = MutableStateFlow(ServiceLocator.sessionStore.isSignedIn)
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    init {
        if (ServiceLocator.sessionStore.isSignedIn) {
            viewModelScope.launch {
                // A failure here needs no message: either the token was
                // rejected (and we are now signed out) or the network is down,
                // in which case the cached profile is still good enough to
                // show while the feed reports the real problem.
                runCatching { authRepository.refreshCurrentMember() }
                _isCheckingSession.value = false
            }
        }
    }

    fun signOut() = authRepository.signOut()
}
