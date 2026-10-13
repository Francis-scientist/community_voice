package com.example.communityvoice.data

import android.content.Context

/**
 * Wires the app's single instances together, once, at startup.
 *
 * A real production app of this size would usually reach for a dependency
 * injection library (Hilt, Koin). This is the same idea in twenty lines: one
 * place that knows how the pieces fit, so a ViewModel can ask for a repository
 * without building a Retrofit client of its own.
 *
 * [init] is called from CommunityVoiceApplication, which Android creates
 * before any Activity or ViewModel exists.
 */
object ServiceLocator {

    private lateinit var applicationContext: Context

    val sessionStore: SessionStore by lazy { SessionStore(applicationContext) }
    private val apiClient: ApiClient by lazy { ApiClient(sessionStore) }
    val api: CommunityVoiceApi by lazy { apiClient.api }

    val authRepository: AuthRepository by lazy { AuthRepository(api, sessionStore) }
    val issueRepository: IssueRepository by lazy { IssueRepository(api) }
    val commentRepository: CommentRepository by lazy { CommentRepository(api) }
    val statsRepository: StatsRepository by lazy { StatsRepository(api) }

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
