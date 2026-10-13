package com.example.communityvoice.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The signed-in person plus the token that proves it. */
data class Session(
    val token: String,
    val member: Member
)

/**
 * Remembers who is signed in, across app restarts.
 *
 * The token and a copy of the member's own record are kept in
 * SharedPreferences - a small key/value file inside the app's private storage,
 * which other apps cannot read on a normal device.
 *
 * Two things worth knowing:
 *
 *  - The password is never stored. Only the token is, and it expires on its
 *    own (30 days by default), after which the backend rejects it and
 *    ApiClient signs the user out.
 *  - On a rooted phone, app-private storage is readable. Anything handling
 *    money or health records should move this to EncryptedSharedPreferences
 *    or the Android Keystore.
 *
 * [session] is a StateFlow, so the whole UI re-draws the moment somebody
 * signs in or out - there is no "restart the app to see your name" step.
 */
class SessionStore(context: Context) {

    private companion object {
        const val PREFS_NAME = "community_voice_session"
        const val KEY_TOKEN = "token"
        const val KEY_MEMBER = "member_json"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _session = MutableStateFlow(readFromDisk())
    val session: StateFlow<Session?> = _session.asStateFlow()

    val current: Session? get() = _session.value
    val token: String? get() = _session.value?.token
    val isSignedIn: Boolean get() = _session.value != null

    /** Called after a successful register or login. */
    fun signIn(response: AuthResponse) {
        write(Session(token = response.token, member = response.user))
    }

    /**
     * Refreshes the stored copy of the member's own details - after they edit
     * their profile, or after the startup call to GET /api/me. Does nothing if
     * nobody is signed in, so a stray response cannot create a session.
     */
    fun updateMember(member: Member) {
        val existing = _session.value ?: return
        write(existing.copy(member = member))
    }

    /** Forgets everything. Called on sign-out, and whenever a token is rejected. */
    fun signOut() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_MEMBER).apply()
        _session.value = null
    }

    private fun write(session: Session) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_MEMBER, gson.toJson(session.member))
            .apply()
        _session.value = session
    }

    private fun readFromDisk(): Session? {
        val token = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val memberJson = prefs.getString(KEY_MEMBER, null) ?: return null

        // A half-written or outdated record is treated as "not signed in"
        // rather than crashing on startup.
        val member = runCatching { gson.fromJson(memberJson, Member::class.java) }.getOrNull()
        if (member == null || member.id.isBlank()) return null

        return Session(token = token, member = member)
    }
}
