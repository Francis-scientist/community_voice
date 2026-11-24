package com.communityvoice.backend.repository

import com.communityvoice.backend.model.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : MongoRepository<Comment, String> {

    /** Oldest first, so a thread reads top to bottom like a conversation. */
    fun findByIssueIdOrderByCreatedAtAsc(issueId: String, pageable: Pageable): Page<Comment>

    fun countByIssueId(issueId: String): Long

    fun countByAuthorId(authorId: String): Long

    fun deleteByIssueId(issueId: String)
}
