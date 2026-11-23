package com.example.communityvoice.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.communityvoice.data.Comment
import com.example.communityvoice.data.Formatters
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.IssueStatus
import com.example.communityvoice.data.MemberRole
import com.example.communityvoice.data.StatusUpdate
import com.example.communityvoice.data.VoteStance
import com.example.communityvoice.ui.components.AuthorRow
import com.example.communityvoice.ui.components.CategoryPill
import com.example.communityvoice.ui.components.ErrorState
import com.example.communityvoice.ui.components.LoadingState
import com.example.communityvoice.ui.components.MemberAvatar
import com.example.communityvoice.ui.components.OfficialBadge
import com.example.communityvoice.ui.components.SectionHeader
import com.example.communityvoice.ui.components.StatusPill
import com.example.communityvoice.ui.components.VoteSplitBar
import com.example.communityvoice.ui.components.statusIcon
import com.example.communityvoice.ui.theme.CommunityVoiceTheme
import com.example.communityvoice.ui.theme.statusColor

/**
 * One issue in full: the whole description, who raised it, how the vote split,
 * the official history, and the discussion.
 *
 * This is where the three questions in the brief are answered most directly -
 * the author's name and avatar sit at the top, and the support and decline
 * counts sit side by side rather than one being hidden behind the other.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    state: IssueDetailUiState,
    isSignedIn: Boolean,
    viewerRole: MemberRole,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onVote: (VoteStance) -> Unit,
    onCommentDraftChanged: (String) -> Unit,
    onPostComment: () -> Unit,
    onDeleteComment: (Comment) -> Unit,
    onLoadMoreComments: () -> Unit,
    onUpdateStatus: (IssueStatus, String) -> Unit,
    onDeleteIssue: () -> Unit,
    onOpenAuthor: (String) -> Unit,
    onShare: (Issue) -> Unit,
    onRequireSignIn: () -> Unit,
    onUserMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.userMessage) {
        val message = state.userMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onUserMessageShown()
        }
    }

    // The issue is gone - leave rather than show an empty shell.
    LaunchedEffect(state.wasDeleted) {
        if (state.wasDeleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val issue = state.issue
                    if (issue != null) {
                        IconButton(onClick = { onShare(issue) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                        if (viewerRole.canModerateStatus) {
                            IconButton(onClick = { showStatusDialog = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Update status")
                            }
                        }
                        if (issue.authoredByViewer) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete issue")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.issue != null) {
                CommentComposer(
                    draft = state.commentDraft,
                    isPosting = state.isPostingComment,
                    canPost = state.canPostComment,
                    isSignedIn = isSignedIn,
                    onDraftChanged = onCommentDraftChanged,
                    onPost = onPostComment,
                    onRequireSignIn = onRequireSignIn
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading && state.issue == null -> LoadingState("Loading issue...")

                state.errorMessage != null && state.issue == null -> ErrorState(
                    title = "Could not load this issue",
                    message = state.errorMessage,
                    onRetry = onRetry
                )

                state.issue != null -> {
                    val issue = state.issue
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            IssueHeader(
                                issue = issue,
                                onOpenAuthor = { onOpenAuthor(issue.author.id) }
                            )
                        }

                        item {
                            VotePanel(
                                issue = issue,
                                isVoting = state.isVoting,
                                isSignedIn = isSignedIn,
                                onVote = onVote,
                                onRequireSignIn = onRequireSignIn
                            )
                        }

                        if (issue.statusHistory.isNotEmpty()) {
                            item { StatusTimeline(issue.statusHistory) }
                        }

                        item {
                            SectionHeader(
                                title = "Discussion",
                                subtitle = if (issue.commentCount == 0) {
                                    "No comments yet"
                                } else if (issue.commentCount == 1) {
                                    "1 comment"
                                } else {
                                    "${issue.commentCount} comments"
                                }
                            )
                        }

                        if (state.comments.isEmpty() && !state.isLoadingComments) {
                            item {
                                Text(
                                    text = "Be the first to add something useful - " +
                                        "when it started, who else it affects, what " +
                                        "has been tried.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(items = state.comments, key = { it.id }) { comment ->
                            CommentRow(
                                comment = comment,
                                onOpenAuthor = { onOpenAuthor(comment.author.id) },
                                onDelete = { onDeleteComment(comment) }
                            )
                        }

                        if (state.hasMoreComments) {
                            item {
                                TextButton(
                                    onClick = onLoadMoreComments,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Load older comments")
                                }
                            }
                        }

                        if (state.isLoadingComments) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        state.commentsError?.let { error ->
                            item {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStatusDialog && state.issue != null) {
        StatusUpdateDialog(
            current = state.issue.issueStatus,
            isSaving = state.isUpdatingStatus,
            onDismiss = { showStatusDialog = false },
            onConfirm = { status, note ->
                onUpdateStatus(status, note)
                showStatusDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this issue?") },
            text = {
                Text(
                    "Every vote and comment on it goes too. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteIssue()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Keep it") }
            }
        )
    }
}

// --------------------------------------------------------------- the header

@Composable
private fun IssueHeader(issue: Issue, onOpenAuthor: () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryPill(issue.issueCategory)
            StatusPill(issue.issueStatus)
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = issue.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (issue.location.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = issue.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Who raised it - tappable, so you can see what else they have said.
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuthorRow(
                    author = issue.author,
                    timestamp = issue.createdAt,
                    avatarSize = 40,
                    onClick = onOpenAuthor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Raised this",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = issue.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ----------------------------------------------------------- the vote panel

/**
 * The support / decline tally and buttons.
 *
 * Both numbers are given equal weight on purpose. An issue that fifty people
 * want and forty people object to is a different thing from one that fifty
 * people want and nobody minds, and hiding the second number would make those
 * two look identical.
 */
@Composable
private fun VotePanel(
    issue: Issue,
    isVoting: Boolean,
    isSignedIn: Boolean,
    onVote: (VoteStance) -> Unit,
    onRequireSignIn: () -> Unit
) {
    val accents = CommunityVoiceTheme.accents
    val vote: (VoteStance) -> Unit = { if (isSignedIn) onVote(it) else onRequireSignIn() }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TallyColumn(
                    count = issue.supportCount,
                    label = "support",
                    tint = accents.support.tint,
                    modifier = Modifier.weight(1f)
                )
                TallyColumn(
                    count = issue.declineCount,
                    label = "declined",
                    tint = accents.decline.tint,
                    modifier = Modifier.weight(1f)
                )
                TallyColumn(
                    count = issue.commentCount,
                    label = if (issue.commentCount == 1) "comment" else "comments",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            VoteSplitBar(
                supportCount = issue.supportCount,
                declineCount = issue.declineCount,
                height = 8
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = when {
                    issue.totalVotes == 0 -> "Nobody has voted yet."
                    else -> "${(issue.supportRatio * 100).toInt()}% of " +
                        "${issue.totalVotes} votes are in support."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigVoteButton(
                    label = if (issue.stance == VoteStance.SUPPORT) "Supporting" else "Support",
                    icon = if (issue.stance == VoteStance.SUPPORT) {
                        Icons.Filled.ThumbUp
                    } else {
                        Icons.Filled.ThumbUpOffAlt
                    },
                    selected = issue.stance == VoteStance.SUPPORT,
                    tint = accents.support.tint,
                    container = accents.support.container,
                    busy = isVoting,
                    onClick = { vote(VoteStance.SUPPORT) },
                    modifier = Modifier.weight(1f)
                )
                BigVoteButton(
                    label = if (issue.stance == VoteStance.DECLINE) "Declined" else "Decline",
                    icon = if (issue.stance == VoteStance.DECLINE) {
                        Icons.Filled.ThumbDown
                    } else {
                        Icons.Filled.ThumbDownOffAlt
                    },
                    selected = issue.stance == VoteStance.DECLINE,
                    tint = accents.decline.tint,
                    container = accents.decline.container,
                    busy = isVoting,
                    onClick = { vote(VoteStance.DECLINE) },
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(visible = issue.stance != null) {
                Text(
                    text = "Tap your choice again to withdraw it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TallyColumn(
    count: Int,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = Formatters.compactCount(count),
            style = MaterialTheme.typography.displaySmall,
            color = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BigVoteButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tint: Color,
    container: Color,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) container else Color.Transparent,
        contentColor = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) tint else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.clickable(enabled = !busy, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// -------------------------------------------------------------- the history

/**
 * The official trail. This is what turns the app from a complaints box into
 * something with accountability: every move is dated and signed.
 */
@Composable
private fun StatusTimeline(history: List<StatusUpdate>) {
    Column {
        SectionHeader(
            title = "Progress",
            subtitle = "Updates from the council"
        )
        Spacer(Modifier.height(10.dp))

        // Newest first, which is the order people read an update trail in.
        history.reversed().forEachIndexed { index, update ->
            TimelineRow(update = update, isLatest = index == 0)
        }
    }
}

@Composable
private fun TimelineRow(update: StatusUpdate, isLatest: Boolean) {
    val status = update.issueStatus
    val color = statusColor(status)

    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = if (isLatest) color else color.copy(alpha = 0.25f),
                shape = CircleShape
            ) {
                Icon(
                    statusIcon(status),
                    contentDescription = null,
                    tint = if (isLatest) Color.White else color,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(14.dp)
                )
            }
            // The connector down to the next entry.
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (update.note.isBlank()) 18.dp else 34.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = status.label,
                style = MaterialTheme.typography.titleSmall,
                color = color
            )
            Text(
                text = buildString {
                    if (update.updatedByName.isNotBlank()) {
                        append(update.updatedByName)
                        append(" · ")
                    }
                    append(Formatters.relativeTime(update.updatedAt))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (update.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = update.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ------------------------------------------------------------- the comments

@Composable
private fun CommentRow(
    comment: Comment,
    onOpenAuthor: () -> Unit,
    onDelete: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        MemberAvatar(
            initials = comment.author.initials,
            colorHex = comment.author.avatarColor,
            size = 32,
            modifier = Modifier.clickable(onClick = onOpenAuthor)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (comment.author.memberRole != MemberRole.CITIZEN) {
                    Spacer(Modifier.width(6.dp))
                    OfficialBadge(comment.author.memberRole)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = Formatters.relativeTime(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (comment.authoredByViewer) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete comment",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The bar pinned to the bottom for writing a comment. */
@Composable
private fun CommentComposer(
    draft: String,
    isPosting: Boolean,
    canPost: Boolean,
    isSignedIn: Boolean,
    onDraftChanged: (String) -> Unit,
    onPost: () -> Unit,
    onRequireSignIn: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column {
            HorizontalDivider()
            if (!isSignedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sign in to join the discussion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onRequireSignIn) { Text("Sign in") }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = onDraftChanged,
                        placeholder = { Text("Add to the discussion") },
                        maxLines = 4,
                        enabled = !isPosting,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onPost, enabled = canPost) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Post comment",
                                tint = if (canPost) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------- official actions

/** Lets an official pick the new stage and leave a note explaining it. */
@Composable
private fun StatusUpdateDialog(
    current: IssueStatus,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (IssueStatus, String) -> Unit
) {
    var selected by remember { mutableStateOf(current) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update progress") },
        text = {
            Column {
                Text(
                    text = "Everybody following this issue sees the new stage and " +
                        "your note.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                IssueStatus.entries.forEach { status ->
                    val isSelected = status == selected
                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = status }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                statusIcon(status),
                                contentDescription = null,
                                tint = statusColor(status),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(status.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. Contractor scheduled for Monday") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected, note) }, enabled = !isSaving) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
