package com.communityvoice.backend.web

import com.communityvoice.backend.service.CommentNotFoundException
import com.communityvoice.backend.service.EmailAlreadyRegisteredException
import com.communityvoice.backend.service.ForbiddenException
import com.communityvoice.backend.service.InvalidCredentialsException
import com.communityvoice.backend.service.InvalidRequestException
import com.communityvoice.backend.service.IssueNotFoundException
import com.communityvoice.backend.service.UnauthorizedException
import com.communityvoice.backend.service.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Turns exceptions into tidy JSON so the Android app always gets a readable
 * `message` field instead of a raw stack trace, plus a stable `error` code it
 * can branch on.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    // ------------------------------------------------------------ 400 group

    /** A required field was blank, too short, or badly formatted. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { fieldError ->
            fieldError.field to (fieldError.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity.badRequest().body(
            ApiErrorResponse(
                error = "VALIDATION_FAILED",
                message = fieldErrors.values.firstOrNull() ?: "Some fields are invalid",
                fieldErrors = fieldErrors
            )
        )
    }

    /** The JSON was malformed, or an enum value was not one we know. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.badRequest().body(
            ApiErrorResponse(
                error = "INVALID_BODY",
                message = "That request body could not be read. Check the field names, " +
                    "and that category, status and stance use one of the allowed values."
            )
        )

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequest(ex: InvalidRequestException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.badRequest().body(
            ApiErrorResponse(
                error = "INVALID_REQUEST",
                message = ex.message ?: "That request could not be understood."
            )
        )

    /** A query parameter was the wrong type - page=abc, for instance. */
    @ExceptionHandler(
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class
    )
    fun handleBadParameter(ex: Exception): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.badRequest().body(
            ApiErrorResponse(
                error = "INVALID_PARAMETER",
                message = "One of the query parameters was not a value this endpoint accepts."
            )
        )

    // ------------------------------------------------------- 401 / 403 / 409

    /**
     * Not signed in. The app treats this as "the stored token is no longer
     * any good" and returns to the welcome screen.
     */
    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiErrorResponse(
                error = "UNAUTHORIZED",
                message = ex.message ?: "Please sign in to do that."
            )
        )

    /** Signed in, but not allowed to do this. */
    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiErrorResponse(
                error = "FORBIDDEN",
                message = ex.message ?: "You are not allowed to do that."
            )
        )

    /**
     * Wrong email or wrong password. 401 with a deliberately vague message -
     * see AuthService.login for why we do not say which one was wrong.
     */
    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleBadCredentials(ex: InvalidCredentialsException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiErrorResponse(
                error = "INVALID_CREDENTIALS",
                message = "That email and password do not match an account."
            )
        )

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun handleDuplicateEmail(
        ex: EmailAlreadyRegisteredException
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiErrorResponse(
                error = "EMAIL_ALREADY_REGISTERED",
                message = ex.message ?: "That email is already registered.",
                fieldErrors = mapOf("email" to "That email is already registered")
            )
        )

    // ------------------------------------------------------------ 404 group

    @ExceptionHandler(IssueNotFoundException::class)
    fun handleIssueNotFound(ex: IssueNotFoundException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse(
                error = "ISSUE_NOT_FOUND",
                message = "That issue no longer exists."
            )
        )

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse(
                error = "USER_NOT_FOUND",
                message = "That member could not be found."
            )
        )

    @ExceptionHandler(CommentNotFoundException::class)
    fun handleCommentNotFound(ex: CommentNotFoundException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse(
                error = "COMMENT_NOT_FOUND",
                message = "That comment no longer exists."
            )
        )

    // ---------------------------------------------------------------- other

    /**
     * Spring's own request errors - unknown URL, wrong HTTP method, and so on.
     * Handled here so they keep their real status (404, 405, ...) instead of
     * falling through to the 500 handler below.
     */
    @ExceptionHandler(ErrorResponseException::class)
    fun handleSpringRequestError(ex: ErrorResponseException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(ex.statusCode).body(
            ApiErrorResponse(
                error = "REQUEST_FAILED",
                message = ex.body.detail ?: "That request could not be handled."
            )
        )

    /** Anything unexpected: log the real cause, send a generic message. */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiErrorResponse> {
        log.error("Unexpected error handling request", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiErrorResponse(
                error = "INTERNAL_ERROR",
                message = "Something went wrong on the server."
            )
        )
    }
}
