package com.communityvoice.backend.service

import com.communityvoice.backend.model.Comment
import com.communityvoice.backend.model.Issue
import com.communityvoice.backend.model.IssueStatus
import com.communityvoice.backend.model.User
import com.communityvoice.backend.repository.CommentRepository
import com.communityvoice.backend.repository.IssueRepository
import com.communityvoice.backend.repository.UserRepository
import com.communityvoice.backend.security.AuthUser
import com.communityvoice.backend.security.JwtService
import com.communityvoice.backend.web.AuthResponse
import com.communityvoice.backend.web.LoginRequest
import com.communityvoice.backend.web.PublicProfileResponse
import com.communityvoice.backend.web.RegisterRequest
import com.communityvoice.backend.web.UpdateProfileRequest
import com.communityvoice.backend.web.UserResponse
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val issueRepository: IssueRepository,
    private val commentRepository: CommentRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val mongoTemplate: MongoTemplate
) {

    /**
     * Creates an account and signs the person straight in, so registering is
     * one step rather than "now please log in with what you just typed".
     */
    fun register(request: RegisterRequest): AuthResponse {
        val email = normaliseEmail(request.email)
        val displayName = request.displayName.trim()

        // Checked up front for a clear message. The unique index on the
        // collection is the real guarantee, in case two people register the
        // same address in the same instant - hence the catch below too.
        if (userRepository.existsByEmail(email)) {
            throw EmailAlreadyRegisteredException(email)
        }

        val user = User(
            displayName = displayName,
            email = email,
            // Only the hash is stored. request.password is never written
            // anywhere, never logged, and cannot be read back out of the hash.
            passwordHash = passwordEncoder.encode(request.password),
            neighbourhood = request.neighbourhood.trim(),
            avatarColor = User.avatarColorFor(email)
        )

        val saved = try {
            userRepository.save(user)
        } catch (duplicate: DuplicateKeyException) {
            throw EmailAlreadyRegisteredException(email)
        }

        return signIn(saved)
    }

    /**
     * Checks an email and password.
     *
     * A wrong email and a wrong password give the exact same error on purpose.
     * Telling somebody "no account has that email" would let a stranger work
     * out who is registered here just by trying addresses.
     */
    fun login(request: LoginRequest): AuthResponse {
        val email = normaliseEmail(request.email)
        val user = userRepository.findByEmail(email)

        if (user == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        touchLastSeen(user)
        return signIn(user)
    }

    /** The signed-in user's own record, email included. */
    fun currentProfile(authUser: AuthUser): UserResponse =
        UserResponse.from(loadUser(authUser.id), includeEmail = true)

    /**
     * Edits the signed-in user's own details.
     *
     * Changing the display name also rewrites the copy of it kept on their
     * issues and comments, so the feed does not keep showing the old name.
     */
    fun updateProfile(authUser: AuthUser, request: UpdateProfileRequest): UserResponse {
        val existing = loadUser(authUser.id)

        val newName = request.displayName?.trim()?.takeIf { it.isNotEmpty() }
        val updated = existing.copy(
            displayName = newName ?: existing.displayName,
            bio = request.bio?.trim() ?: existing.bio,
            neighbourhood = request.neighbourhood?.trim() ?: existing.neighbourhood
        )
        val saved = userRepository.save(updated)

        if (newName != null && newName != existing.displayName) {
            renameAuthorEverywhere(authUser.id, newName)
        }

        return UserResponse.from(saved, includeEmail = true)
    }

    /**
     * Anybody's public profile, plus what they have contributed. This is what
     * opens when you tap the name on an issue card.
     */
    fun publicProfile(userId: String): PublicProfileResponse {
        val user = loadUser(userId)
        val theirIssues = mongoTemplate.find(
            Query(Criteria.where("authorId").`is`(userId)),
            Issue::class.java
        )

        return PublicProfileResponse(
            user = UserResponse.from(user, includeEmail = false),
            issuesPosted = theirIssues.size.toLong(),
            commentsPosted = commentRepository.countByAuthorId(userId),
            supportReceived = theirIssues.sumOf { it.supportCount },
            issuesResolved = theirIssues.count { it.status == IssueStatus.RESOLVED }
        )
    }

    // ------------------------------------------------------------- internals

    private fun signIn(user: User): AuthResponse {
        val userId = requireNotNull(user.id) { "a saved user always has an id" }
        val payload = jwtService.issueToken(userId)
        return AuthResponse(
            token = jwtService.encode(payload),
            expiresAt = payload.expiresAt,
            user = UserResponse.from(user, includeEmail = true)
        )
    }

    private fun loadUser(userId: String): User =
        userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }

    private fun normaliseEmail(email: String): String = email.trim().lowercase()

    /** One small write, rather than re-saving the whole user document. */
    private fun touchLastSeen(user: User) {
        val userId = user.id ?: return
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(userId)),
            Update().set("lastSeenAt", Instant.now()),
            User::class.java
        )
    }

    private fun renameAuthorEverywhere(userId: String, newName: String) {
        val byAuthor = Query(Criteria.where("authorId").`is`(userId))
        val rename = Update().set("authorName", newName)
        mongoTemplate.updateMulti(byAuthor, rename, Issue::class.java)
        mongoTemplate.updateMulti(byAuthor, rename, Comment::class.java)
    }
}
