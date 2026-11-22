package com.example.communityvoice.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.communityvoice.data.Formatters
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.IssueStatus
import com.example.communityvoice.data.VoteStance
import com.example.communityvoice.ui.components.AuthorRow
import com.example.communityvoice.ui.components.CategoryPill
import com.example.communityvoice.ui.components.StatusPill
import com.example.communityvoice.ui.components.VoteSplitBar
import com.example.communityvoice.ui.theme.CommunityVoiceTheme

/**
 * One issue in the feed.
 *
 * The card answers four questions at a glance: what the issue is, who raised
 * it, where it has got to, and how the community split on it. The vote buttons
 * sit at the bottom and show the viewer's own choice as a filled icon, so you
 * can tell at a distance whether you have already had your say.
 */
@Composable
fun IssueCard(
    issue: Issue,
    isVoting: Boolean,
    onOpen: () -> Unit,
    onSupport: () -> Unit,
    onDecline: () -> Unit,
    onOpenAuthor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- who and when, plus the lifecycle stage on the right -------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AuthorRow(
                    author = issue.author,
                    timestamp = issue.createdAt,
                    avatarSize = 34,
                    onClick = onOpenAuthor,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // An OPEN issue is the default, so showing a pill for it would
                // be noise on every single card.
                if (issue.issueStatus != IssueStatus.OPEN) {
                    Spacer(Modifier.width(8.dp))
                    StatusPill(issue.issueStatus)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryPill(issue.issueCategory)
                if (issue.location.isNotBlank()) {
                    LocationLabel(issue.location)
                }
            }

            Spacer(Modifier.height(14.dp))

            VoteSplitBar(
                supportCount = issue.supportCount,
                declineCount = issue.declineCount
            )

            Spacer(Modifier.height(10.dp))

            // --- the tally, in words --------------------------------------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Formatters.supportSentence(issue.supportCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (issue.declineCount > 0) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (issue.declineCount == 1) {
                            "1 declined"
                        } else {
                            "${issue.declineCount} declined"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = CommunityVoiceTheme.accents.decline.tint
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- the three actions ---------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoteButton(
                    count = issue.supportCount,
                    stance = VoteStance.SUPPORT,
                    selected = issue.stance == VoteStance.SUPPORT,
                    busy = isVoting,
                    onClick = onSupport,
                    modifier = Modifier.weight(1f)
                )
                VoteButton(
                    count = issue.declineCount,
                    stance = VoteStance.DECLINE,
                    selected = issue.stance == VoteStance.DECLINE,
                    busy = isVoting,
                    onClick = onDecline,
                    modifier = Modifier.weight(1f)
                )
                CommentCountButton(
                    count = issue.commentCount,
                    onClick = onOpen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * A support or decline button.
 *
 * Selected means "this is your vote": the icon fills in and the pill takes the
 * matching colour. Tapping it again withdraws the vote, which the feed handles.
 */
@Composable
private fun VoteButton(
    count: Int,
    stance: VoteStance,
    selected: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = CommunityVoiceTheme.accents
    val accent = if (stance == VoteStance.SUPPORT) accents.support else accents.decline

    val container: Color = if (selected) accent.container else Color.Transparent
    val content: Color = if (selected) accent.tint else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = when {
        stance == VoteStance.SUPPORT && selected -> Icons.Filled.ThumbUp
        stance == VoteStance.SUPPORT -> Icons.Filled.ThumbUpOffAlt
        selected -> Icons.Filled.ThumbDown
        else -> Icons.Filled.ThumbDownOffAlt
    }
    val label = if (stance == VoteStance.SUPPORT) "Support" else "Decline"

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.dp,
            if (selected) accent.tint else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.clickable(enabled = !busy, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = content
                )
            } else {
                Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = Formatters.compactCount(count),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CommentCountButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Comment,
                contentDescription = "Comments",
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = Formatters.compactCount(count),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun LocationLabel(location: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Place,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = location,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Shown under the last card while the next page is on its way. */
@Composable
fun LoadingMoreRow(visible: Boolean) {
    AnimatedVisibility(visible = visible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Loading more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
