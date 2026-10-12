package com.example.communityvoice.data

import com.example.communityvoice.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the one shared Retrofit client the whole app uses.
 *
 * The server address comes from BuildConfig.API_BASE_URL, which is generated
 * from the API_BASE_URL line in android/gradle.properties. To point the app at
 * a different server, edit that line and rebuild - you never need to change
 * this file.
 */
class ApiClient(private val sessionStore: SessionStore) {

    companion object {
        /** e.g. "http://10.0.2.2:8080/" - always ends with a slash. */
        val baseUrl: String = BuildConfig.API_BASE_URL.let {
            if (it.endsWith("/")) it else "$it/"
        }
    }

    /**
     * Attaches the login token to every outgoing request.
     *
     * Doing it here rather than in each API method means a new endpoint is
     * authenticated by default - you cannot forget it.
     */
    private val authInterceptor = Interceptor { chain ->
        val token = sessionStore.token
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    /**
     * Signs the user out the moment the server says their token is no good.
     *
     * A 401 on any normal endpoint means the token expired, or the account was
     * removed. Rather than show a confusing error on every screen, we clear
     * the session, and because SessionStore is a StateFlow the UI slides back
     * to the welcome screen on its own.
     *
     * The /api/auth/ paths are left alone: a 401 there just means somebody
     * mistyped their password on the login form, which is not a stale session.
     */
    private val sessionExpiryInterceptor = Interceptor { chain ->
        val response: Response = chain.proceed(chain.request())
        val path = chain.request().url.encodedPath
        if (response.code == 401 && !path.startsWith("/api/auth/")) {
            sessionStore.signOut()
        }
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Print requests and responses to Logcat in debug builds only.
        // HEADERS rather than BODY so a password typed into the sign-up form
        // never ends up in the log.
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.HEADERS
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(sessionExpiryInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: CommunityVoiceApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CommunityVoiceApi::class.java)
}
