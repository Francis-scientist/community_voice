package com.example.communityvoice.ui.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.communityvoice.data.Author
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.IssueCategory
import com.example.communityvoice.data.Member
import com.example.communityvoice.data.VoteStance
import com.example.communityvoice.ui.components.categoryIcon
import com.example.communityvoice.ui.feed.IssueCard
import com.example.communityvoice.ui.theme.categoryColors

/**
 * The form for raising an issue.
 *
 * Two things here go beyond a plain form. The category picker shows each
 * option in its own colour and icon, the same ones the feed uses, so the
 * choice is recognisable rather than a word in a list. And "Preview" renders
 * the real feed card with the typed content in it, so somebody can see how
 * their report will read to a neighbour before they post it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateIssueScreen(
    state: NewIssueFormState,
    author: Member,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onCategorySelected: (IssueCategory) -> Unit,
    onTogglePreview: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raise an issue") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.canPreview) {
                        IconButton(onClick = onTogglePreview) {
                            Icon(
                                if (state.showPreview) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (state.showPreview) {
                                    "Hide preview"
                                } else {
                                    "Show preview"
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            WritingTip()

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChanged,
                label = { Text("Title") },
                placeholder = { Text("e.g. Potholes on Market Road") },
                singleLine = true,
                isError = state.titleError != null,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(state.titleError ?: "Short and specific works best")
                        Text("${state.titleCharactersLeft}")
                    }
                },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChanged,
                label = { Text("Description") },
                placeholder = { Text("What is the problem, and who does it affect?") },
                minLines = 5,
                maxLines = 10,
                isError = state.descriptionError != null,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            state.descriptionError
                                ?: "Include when it started and who it affects"
                        )
                        Text("${state.descriptionCharactersLeft}")
                    }
                },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.location,
                onValueChange = onLocationChanged,
                label = { Text("Where is it? (optional)") },
                placeholder = { Text("e.g. Market Road, near the junction") },
                leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
                singleLine = true,
                supportingText = { Text("Helps whoever has to find it") },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Text("Category", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IssueCategory.entries.forEach { category ->
                    CategoryChoice(
                        category = category,
                        selected = state.category == category,
                        enabled = !state.isSubmitting,
                        onClick = { onCategorySelected(category) }
                    )
                }
            }

            if (state.categoryError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.categoryError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            AnimatedVisibility(visible = state.showPreview && state.canPreview) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Preview", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "how neighbours will see it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    IssuePreviewCard(state = state, author = author)
                }
            }

            if (state.submitError != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.submitError,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Posting...")
                } else {
                    Text("Post issue", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Posted as ${author.displayName}. You count as its first supporter, " +
                    "which you can withdraw later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** One tappable category, in its own colour. */
@Composable
private fun CategoryChoice(
    category: IssueCategory,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = categoryColors(category)
    Surface(
        color = if (selected) colors.container else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) colors.tint else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) colors.tint else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                categoryIcon(category),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * The real feed card, filled in with what has been typed so far.
 *
 * Reusing [IssueCard] rather than drawing a lookalike means the preview can
 * never drift out of step with the feed.
 */
@Composable
private fun IssuePreviewCard(state: NewIssueFormState, author: Member) {
    val preview = Issue(
        id = "preview",
        title = state.title.trim(),
        description = state.description.trim(),
        category = (state.category ?: IssueCategory.OTHER).apiValue,
        location = state.location.trim(),
        author = Author(
            id = author.id,
            displayName = author.displayName,
            initials = author.initials,
            avatarColor = author.avatarColor,
            role = author.role
        ),
        // The author counts as the first supporter, which is what the backend
        // does on create - so the preview matches what will actually appear.
        supportCount = 1,
        myVote = VoteStance.SUPPORT.apiValue,
        createdAt = ""
    )

    IssueCard(
        issue = preview,
        isVoting = false,
        onOpen = {},
        onSupport = {},
        onDecline = {},
        onOpenAuthor = {}
    )
}

/** A short nudge towards a report somebody can act on. */
@Composable
private fun WritingTip() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(
                Icons.Filled.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "What makes a report work",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "One problem, where it is, how long it has been like " +
                        "that, and who it affects. Specific beats angry.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
