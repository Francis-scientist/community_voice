package com.example.communityvoice.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.communityvoice.data.CategoryStat
import com.example.communityvoice.data.CommunityStats
import com.example.communityvoice.data.ContributorStat
import com.example.communityvoice.data.Formatters
import com.example.communityvoice.data.IssueCategory
import com.example.communityvoice.data.IssueStatus
import com.example.communityvoice.data.StatusStat
import com.example.communityvoice.ui.components.ErrorState
import com.example.communityvoice.ui.components.LoadingState
import com.example.communityvoice.ui.components.MemberAvatar
import com.example.communityvoice.ui.components.SectionHeader
import com.example.communityvoice.ui.components.StatTile
import com.example.communityvoice.ui.components.categoryIcon
import com.example.communityvoice.ui.components.statusIcon
import com.example.communityvoice.ui.theme.categoryColors
import com.example.communityvoice.ui.theme.statusColor

/**
 * The Insights screen - what the community as a whole has been doing.
 *
 * A few deliberate choices about how this is drawn:
 *
 *  - **One hero number.** The total issues raised leads the screen; everything
 *    else is a smaller tile. Two competing hero numbers would mean neither is
 *    the headline.
 *  - **The category bars are all one colour.** Bar length already encodes the
 *    count, so colouring each bar differently would spend the only free channel
 *    restating what the length says. Identity comes from the category's own
 *    icon beside the label - the same icon and colour the feed uses - so a
 *    reader who learned "Water is blue" still sees it here.
 *  - **The status rows keep their lifecycle colours**, each with its icon and
 *    its label, so the stage never depends on colour alone.
 *  - **Every value is written next to its bar.** Nothing on this screen can
 *    only be read by comparing lengths or hues.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenCategory: (IssueCategory) -> Unit,
    onOpenMember: (String) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Insights") }) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading && state.stats == null -> LoadingState("Counting up...")

                state.errorMessage != null && state.stats == null -> ErrorState(
                    title = "Could not load insights",
                    message = state.errorMessage,
                    onRetry = onRetry
                )

                state.stats != null -> {
                    val stats = state.stats
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            item { HeroFigure(stats) }
                            item { KpiRow(stats) }
                            item { ResolutionMeter(stats) }
                            item { CategoryBreakdown(stats.byCategory, onOpenCategory) }
                            item { StatusFunnel(stats.byStatus) }
                            item { Leaderboard(stats.topContributors, onOpenMember) }
                            item { Footnote(stats) }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------- the hero number

/**
 * The one number the screen leads with. Same typeface as everything else - a
 * display face here would read as decoration rather than data.
 */
@Composable
private fun HeroFigure(stats: CommunityStats) {
    Column {
        Text(
            text = "Issues raised",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stats.totalIssues.toString(),
            // Proportional figures on purpose: tabular digits make a number
            // like 121 look loose at this size.
            style = TextStyle(
                fontSize = 48.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (stats.issuesThisWeek == 0L) {
                "None raised in the last 7 days"
            } else {
                "${stats.issuesThisWeek} raised in the last 7 days"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Three supporting tiles. Each is a number, not a one-bar chart. */
@Composable
private fun KpiRow(stats: CommunityStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            value = Formatters.compactCount(stats.totalMembers),
            label = "Members",
            icon = Icons.Filled.Groups,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = Formatters.compactCount(stats.totalSupports),
            label = "Votes in support",
            icon = Icons.Filled.ThumbUp,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = Formatters.compactCount(stats.totalComments),
            label = "Comments",
            icon = Icons.Filled.ChatBubbleOutline,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------- the meter

/**
 * One ratio against a limit, so a meter rather than a chart.
 *
 * The unfilled track is a lighter step of the same hue as the fill, so the
 * whole bar reads as one measurement rather than two unrelated colours.
 */
@Composable
private fun ResolutionMeter(stats: CommunityStats) {
    val fraction by animateFloatAsState(
        targetValue = (stats.resolutionRatePercent / 100f).coerceIn(0f, 1f),
        label = "resolutionRate"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Resolved",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stats.resolutionRatePercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "${stats.resolvedIssues} of ${stats.totalIssues} issues are " +
                    "resolved. ${stats.inProgressIssues} are being worked on and " +
                    "${stats.openIssues} are still waiting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --------------------------------------------------------- the category bars

@Composable
private fun CategoryBreakdown(
    rows: List<CategoryStat>,
    onOpenCategory: (IssueCategory) -> Unit
) {
    // Scaled against the biggest bar rather than the total, so the shape of
    // the comparison is visible even when one category dominates.
    val maxCount = rows.maxOfOrNull { it.issueCount }?.coerceAtLeast(1L) ?: 1L

    Column {
        SectionHeader(
            title = "By category",
            subtitle = "Issues raised in each area. Tap one to filter the feed."
        )
        Spacer(Modifier.height(12.dp))

        rows.forEach { row ->
            CategoryBarRow(
                row = row,
                maxCount = maxCount,
                onClick = { onOpenCategory(row.issueCategory) }
            )
        }
    }
}

@Composable
private fun CategoryBarRow(
    row: CategoryStat,
    maxCount: Long,
    onClick: () -> Unit
) {
    val category = row.issueCategory
    val accent = categoryColors(category)
    val fraction by animateFloatAsState(
        targetValue = (row.issueCount.toFloat() / maxCount).coerceIn(0f, 1f),
        label = "categoryBar"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The icon in the category's own colour is what carries identity -
            // not the bar, which is one hue for every row.
            Icon(
                categoryIcon(category),
                contentDescription = null,
                tint = accent.tint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = category.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // Value at the tip of the bar - so nothing here is readable only
            // by comparing lengths.
            Text(
                text = row.issueCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // A category with no issues shows the empty track and nothing
            // else, rather than a misleading sliver.
            if (row.issueCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        // Rounded at the data end, square at the baseline.
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

// -------------------------------------------------------- the status funnel

/**
 * Where issues currently sit in their lifecycle.
 *
 * These keep the colours they wear everywhere else in the app, and each row
 * carries its icon and its name, so the stage never depends on the colour.
 */
@Composable
private fun StatusFunnel(rows: List<StatusStat>) {
    val total = rows.sumOf { it.issueCount }.coerceAtLeast(1L)

    Column {
        SectionHeader(
            title = "Where issues stand",
            subtitle = "Every issue, by the stage it has reached"
        )
        Spacer(Modifier.height(12.dp))

        rows.forEach { row ->
            StatusRow(row = row, total = total)
        }
    }
}

@Composable
private fun StatusRow(row: StatusStat, total: Long) {
    val status: IssueStatus = row.issueStatus
    val color = statusColor(status)
    val fraction by animateFloatAsState(
        targetValue = (row.issueCount.toFloat() / total).coerceIn(0f, 1f),
        label = "statusBar"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            statusIcon(status),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(96.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (row.issueCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(color)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = row.issueCount.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ----------------------------------------------------------- the leaderboard

@Composable
private fun Leaderboard(
    contributors: List<ContributorStat>,
    onOpenMember: (String) -> Unit
) {
    if (contributors.isEmpty()) return

    Column {
        SectionHeader(
            title = "Most active members",
            subtitle = "By how many issues they have raised"
        )
        Spacer(Modifier.height(12.dp))

        contributors.forEachIndexed { index, contributor ->
            ContributorRow(
                rank = index + 1,
                contributor = contributor,
                onClick = { onOpenMember(contributor.userId) }
            )
        }
    }
}

@Composable
private fun ContributorRow(
    rank: Int,
    contributor: ContributorStat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp)
        )
        if (rank == 1) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = "Top contributor",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        } else {
            Spacer(Modifier.width(22.dp))
        }
        MemberAvatar(
            initials = contributor.initials,
            colorHex = contributor.avatarColor,
            size = 34
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contributor.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = buildString {
                    append(contributor.issuesPosted)
                    append(if (contributor.issuesPosted == 1L) " issue" else " issues")
                    append(" · ")
                    append(Formatters.compactCount(contributor.supportReceived))
                    append(" support")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Footnote(stats: CommunityStats) {
    Column {
        Text(
            text = "${Formatters.compactCount(stats.totalDeclines)} votes have been " +
                "cast against issues. Disagreement is counted and shown, not hidden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Figures from ${Formatters.relativeTime(stats.generatedAt)}.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}
