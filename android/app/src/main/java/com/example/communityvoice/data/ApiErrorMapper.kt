package com.example.communityvoice.data

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/**
 * A failed call, in a form the screens can act on.
 *
 * [code] is the backend's stable machine code (VALIDATION_FAILED,
 * EMAIL_ALREADY_REGISTERED, ...), so a screen can react to a specific case.
 * [message] is the sentence to show. [fieldErrors] maps a form field name to
 * its own message, which is what lets the sign-up form put "That email is
 * already registered" directly under the email box.
 */
data class ApiFailure(
    val code: String,
    val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    val httpStatus: Int? = null
) {
    /** True when the phone could not reach the server at all. */
    val isNetworkProblem: Boolean get() = code == CODE_NETWORK

    /** True when the token was rejected - the app has already signed out. */
    val isSessionExpired: Boolean get() = httpStatus == 401 && code != "INVALID_CREDENTIALS"

    companion object {
        const val CODE_NETWORK = "NETWORK_UNREACHABLE"
        const val CODE_UNKNOWN = "UNKNOWN"
    }
}

/**
 * Turns an exception into something we can show the user.
 *
 * Retrofit throws:
 *   - IOException      when the phone could not reach the server at all
 *   - HttpException    when the server replied with an error status (4xx/5xx)
 */
object ApiErrorMapper {

    private val gson = Gson()

    fun toFailure(throwable: Throwable): ApiFailure = when (throwable) {
        is HttpException -> fromHttpException(throwable)

        is IOException -> ApiFailure(
            code = ApiFailure.CODE_NETWORK,
            message = "Could not reach the server.\n\n" +
                "Check that the backend is running and that API_BASE_URL is correct " +
                "(currently ${ApiClient.baseUrl})."
        )

        else -> ApiFailure(
            code = ApiFailure.CODE_UNKNOWN,
            message = throwable.message ?: "Something went wrong."
        )
    }

    /** Shorthand for the many places that only want the sentence. */
    fun toUserMessage(throwable: Throwable): String = toFailure(throwable).message

    private fun fromHttpException(exception: HttpException): ApiFailure {
        // The backend sends { "error", "message", "fieldErrors" } - prefer its
        // wording, since it knows what actually went wrong.
        val parsed = runCatching {
            val rawBody = exception.response()?.errorBody()?.string()
            if (rawBody.isNullOrBlank()) {
                null
            } else {
                gson.fromJson(rawBody, ApiErrorResponse::class.java)
            }
        }.getOrNull()

        if (parsed != null && parsed.message.isNotBlank()) {
            return ApiFailure(
                code = parsed.error.ifBlank { ApiFailure.CODE_UNKNOWN },
                message = parsed.message,
                fieldErrors = parsed.fieldErrors,
                httpStatus = exception.code()
            )
        }

        // No usable body - fall back to something sensible per status code.
        val fallback = when (exception.code()) {
            401 -> "Please sign in again."
            403 -> "You are not allowed to do that."
            404 -> "That item no longer exists."
            409 -> "That conflicts with something already here."
            in 500..599 -> "The server had a problem. Please try again shortly."
            else -> "Request failed (HTTP ${exception.code()})."
        }
        return ApiFailure(
            code = ApiFailure.CODE_UNKNOWN,
            message = fallback,
            httpStatus = exception.code()
        )
    }
}
