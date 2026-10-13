package com.example.communityvoice.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.communityvoice.data.Author
import com.example.communityvoice.data.Formatters
import com.example.communityvoice.data.IssueCategory
import com.example.communityvoice.data.IssueStatus
import com.example.communityvoice.data.MemberRole
import com.example.communityvoice.ui.theme.CommunityVoiceTheme
import com.example.communityvoice.ui.theme.categoryColors
import com.example.communityvoice.ui.theme.statusColor

/**
 * The small pieces every screen is built from - avatars, pills, the vote bar,
 * and the loading / empty / error placeholders.
 *
 * Keeping them here means a change to, say, how a category pill looks happens
 * once rather than in five screens.
 */

// ------------------------------------------------------------------ avatars

/**
 * Turns a "#1B6B4A" string from the server into a Color.
 * Falls back to the theme's primary if the value is not a valid hex colour,
 * so a bad record can never crash a list.
 */
@Composable
fun parseHexColor(hex: String): Color {
    val fallback = MaterialTheme.colorScheme.primary
    val cleaned = hex.removePrefix("#")
    if (cleaned.length != 6) return fallback
    val value = cleaned.toLongOrNull(16) ?: return fallback
    return Color(value or 0xFF000000L)
}

/** The initials circle used wherever a person is shown. */
@Composable
fun MemberAvatar(
    initials: String,
    colorHex: String,
    size: Int = 40,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(parseHexColor(colorHex)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(2).ifBlank { "?" },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            // Scales with the circle so one component covers 28dp list
            // avatars and 72dp profile headers.
            fontSize = (size * 0.38f).sp
        )
    }
}

/** Avatar + name + optional "Official" badge + a timestamp. */
@Composable
fun AuthorRow(
    author: Author,
    timestamp: String,
    modifier: Modifier = Modifier,
    avatarSize: Int = 36,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = rowModifier) {
        MemberAvatar(
            initials = author.initials,
            colorHex = author.avatarColor,
            size = avatarSize
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = author.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (author.memberRole != MemberRole.CITIZEN) {
                    Spacer(Modifier.width(5.dp))
                    OfficialBadge(author.memberRole)
                }
            }
            Text(
                text = Formatters.relativeTime(timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Marks a council worker or administrator, so a status update visibly comes
 * from somebody with the authority to make it.
 */
@Composable
fun OfficialBadge(role: MemberRole) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                Icons.Filled.Verified,
                contentDescription = null,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(role.label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// -------------------------------------------------------------------- pills

/** The icon that stands for each category. */
fun categoryIcon(category: IssueCategory): ImageVector = when (category) {
    IssueCategory.ROADS -> Icons.Filled.Construction
    IssueCategory.WATER -> Icons.Filled.WaterDrop
    IssueCategory.ELECTRICITY -> Icons.Filled.Bolt
    IssueCategory.SANITATION -> Icons.Filled.CleaningServices
    IssueCategory.HEALTH -> Icons.Filled.LocalHospital
    IssueCategory.SCHOOLS -> Icons.Filled.School
    IssueCategory.SAFETY -> Icons.Filled.Shield
    IssueCategory.OTHER -> Icons.Filled.MoreHoriz
}

fun statusIcon(status: IssueStatus): ImageVector = when (status) {
    IssueStatus.OPEN -> Icons.Filled.RadioButtonUnchecked
    IssueStatus.UNDER_REVIEW -> Icons.Filled.Visibility
    IssueStatus.IN_PROGRESS -> Icons.Filled.Autorenew
    IssueStatus.RESOLVED -> Icons.Filled.CheckCircle
    IssueStatus.DECLINED -> Icons.Filled.Cancel
}

@Composable
fun CategoryPill(category: IssueCategory, modifier: Modifier = Modifier) {
    val colors = categoryColors(category)
    Pill(
        text = category.label,
        icon = categoryIcon(category),
        tint = colors.tint,
        container = colors.container,
        modifier = modifier
    )
}

@Composable
fun StatusPill(status: IssueStatus, modifier: Modifier = Modifier) {
    val tint = statusColor(status)
    Pill(
        text = status.label,
        icon = statusIcon(status),
        tint = tint,
        // A faint wash of the same hue, so the pill reads as related to the
        // icon without needing a second colour defined per status.
        container = tint.copy(alpha = 0.14f),
        modifier = modifier
    )
}

@Composable
private fun Pill(
    text: String,
    icon: ImageVector,
    tint: Color,
    container: Color,
    modifier: Modifier = Modifier
) {
    Surface(color = container, contentColor = tint, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ----------------------------------------------------------------- vote bar

/**
 * A single bar showing how the vote split: support from the left, declines
 * from the right. One glance tells you both how much feeling an issue has
 * attracted and whether that feeling is one-sided.
 *
 * An issue nobody has voted on shows a flat neutral track rather than a
 * misleading 50/50 split.
 */
@Composable
fun VoteSplitBar(
    supportCount: Int,
    declineCount: Int,
    modifier: Modifier = Modifier,
    height: Int = 6
) {
    val accents = CommunityVoiceTheme.accents
    val total = supportCount + declineCount
    val target = if (total == 0) 0f else supportCount.toFloat() / total
    // Animated so the bar slides when a vote lands instead of jumping.
    val fraction by animateFloatAsState(targetValue = target, label = "supportFraction")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (total == 0) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    accents.decline.tint.copy(alpha = 0.35f)
                }
            )
    ) {
        if (total > 0 && fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(accents.support.tint)
            )
        }
    }
}

// ------------------------------------------------------------- stat tiles

/** One headline number with a caption, used across the Insights screen. */
@Composable
fun StatTile(
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A small heading above a block of content. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

// ----------------------------------------------------- loading / empty / error

@Composable
fun LoadingState(message: String = "Loading...", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(18.dp)
                    .size(34.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong"
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRetry) { Text("Try again") }
    }
}
