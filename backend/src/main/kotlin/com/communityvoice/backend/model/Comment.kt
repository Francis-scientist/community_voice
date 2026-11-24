package com.communityvoice.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * One comment on an issue, in the MongoDB "comments" collection.
 *
 * Comments live in their own collection rather than inside the issue so that
 * loading the feed never drags along thousands of lines of discussion.
 * The issue keeps only a running [Issue.commentCount].
 */
@Document(collection = "comments")
@CompoundIndex(name = "issue_created_idx", def = "{'issueId': 1, 'createdAt': 1}")
data class Comment(
    @Id
    val id: String? = null,

    val issueId: String,

    val body: String,

    val authorId: String,

    val authorName: String,

    val authorAvatarColor: String = "#1B6B4A",

    val authorRole: UserRole = UserRole.CITIZEN,

    val createdAt: Instant = Instant.now()
)
