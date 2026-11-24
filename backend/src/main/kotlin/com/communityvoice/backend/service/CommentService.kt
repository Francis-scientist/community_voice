package com.communityvoice.backend.service

import com.communityvoice.backend.model.Comment
import com.communityvoice.backend.repository.CommentRepository
import com.communityvoice.backend.security.AuthUser
import com.communityvoice.backend.web.CreateCommentRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val issueService: IssueService
) {

    companion object {
        private const val MAX_PAGE_SIZE = 50
    }

    /** Oldest first, so a thread reads like a conversation. */
    fun listComments(issueId: String, page: Int, size: Int): Page<Comment> {
        // Reading comments for an issue that does not exist is a 404, not an
        // empty list - otherwise a typo in the id looks like a quiet thread.
        issueService.getIssue(issueId)
        return commentRepository.findByIssueIdOrderByCreatedAtAsc(
            issueId,
            PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        )
    }

    fun addComment(issueId: String, request: CreateCommentRequest, author: AuthUser): Comment {
        // Throws 404 if the issue is gone, so we never store an orphan.
        issueService.getIssue(issueId)

        val saved = commentRepository.save(
            Comment(
                issueId = issueId,
                body = request.body.trim(),
                authorId = author.id,
                authorName = author.displayName,
                authorAvatarColor = author.avatarColor,
                authorRole = author.role
            )
        )
        issueService.adjustCommentCount(issueId, 1)
        return saved
    }

    /** Only the person who wrote a comment, or an admin, can remove it. */
    fun deleteComment(commentId: String, requester: AuthUser) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { CommentNotFoundException(commentId) }

        val isOwner = comment.authorId == requester.id
        if (!isOwner && !requester.role.canDeleteAnything) {
            throw ForbiddenException("You can only delete your own comments.")
        }

        commentRepository.deleteById(commentId)
        issueService.adjustCommentCount(comment.issueId, -1)
    }
}
