package com.example.communityvoice.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The backend API, described as a Kotlin interface.
 *
 * Retrofit reads these annotations and writes the real HTTP code for us.
 * `suspend` means each call can be made from a coroutine without blocking
 * the screen.
 *
 * None of these methods takes a token. ApiClient's AuthInterceptor attaches
 * `Authorization: Bearer ...` to every request automatically, so there is no
 * way to forget it on a new endpoint.
 */
interface CommunityVoiceApi {

    // ------------------------------------------------------------------ auth

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    /** Confirms the stored token still works, and refreshes the local copy. */
    @GET("api/me")
    suspend fun me(): Member

    @PATCH("api/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Member

    @GET("api/users/{id}")
    suspend fun publicProfile(@Path("id") userId: String): PublicProfile

    // ----------------------------------------------------------------- feed

    @GET("api/issues")
    suspend fun getIssues(
        @Query("q") search: String? = null,
        @Query("category") category: String? = null,
        @Query("status") status: String? = null,
        @Query("author") author: String? = null,
        @Query("votedBy") votedBy: String? = null,
        @Query("sort") sort: String = "trending",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PagedResponse<Issue>

    @GET("api/issues/{id}")
    suspend fun getIssue(@Path("id") issueId: String): Issue

    @POST("api/issues")
    suspend fun createIssue(@Body request: CreateIssueRequest): Issue

    @DELETE("api/issues/{id}")
    suspend fun deleteIssue(@Path("id") issueId: String)

    @PUT("api/issues/{id}/vote")
    suspend fun vote(
        @Path("id") issueId: String,
        @Body request: VoteRequest
    ): Issue

    @DELETE("api/issues/{id}/vote")
    suspend fun clearVote(@Path("id") issueId: String): Issue

    @PATCH("api/issues/{id}/status")
    suspend fun updateStatus(
        @Path("id") issueId: String,
        @Body request: UpdateStatusRequest
    ): Issue

    // ------------------------------------------------------------- comments

    @GET("api/issues/{id}/comments")
    suspend fun getComments(
        @Path("id") issueId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): PagedResponse<Comment>

    @POST("api/issues/{id}/comments")
    suspend fun addComment(
        @Path("id") issueId: String,
        @Body request: CreateCommentRequest
    ): Comment

    @DELETE("api/comments/{id}")
    suspend fun deleteComment(@Path("id") commentId: String)

    // ---------------------------------------------------------------- stats

    @GET("api/stats")
    suspend fun getStats(): CommunityStats
}
