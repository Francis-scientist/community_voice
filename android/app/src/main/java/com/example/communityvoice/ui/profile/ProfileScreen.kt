package com.example.communityvoice.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.communityvoice.data.Formatters
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.MemberRole
import com.example.communityvoice.data.PublicProfile
import com.example.communityvoice.data.VoteStance
import com.example.communityvoice.ui.components.ErrorState
import com.example.communityvoice.ui.components.LoadingState
import com.example.communityvoice.ui.components.MemberAvatar
import com.example.communityvoice.ui.components.OfficialBadge
import com.example.communityvoice.ui.components.SectionHeader
import com.example.communityvoice.ui.feed.IssueCard

/**
 * One member's profile.
 *
 * Tapping a name anywhere in the app lands here, which is what makes "who
 * raised this issue" a real answer rather than just a label: you can see what
 * else they have raised, how much support it drew, and how much of it got
 * fixed.
 *
 * [isSelf] turns on the edit and sign-out controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    isSelf: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onEditNameChanged: (String) -> Unit,
    onEditBioChanged: (String) -> Unit,
    onEditNeighbourhoodChanged: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onSignOut: () -> Unit,
    onOpenIssue: (Issue) -> Unit,
    onVote: (Issue, VoteStance) -> Unit,
    onUserMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.userMessage) {
        val message = state.userMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onUserMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelf) "Your profile" else "Member") },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (isSelf && state.profile != null) {
                        IconButton(onClick = onStartEditing) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                        }
                        IconButton(onClick = { showSignOutDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading && state.profile == null -> LoadingState("Loading profile...")

                state.errorMessage != null && state.profile == null -> ErrorState(
                    title = "Could not load this profile",
                    message = state.errorMessage,
                    onRetry = onRetry
                )

                state.profile != null -> {
                    val profile = state.profile
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { ProfileHeader(profile) }
                        item { ContributionRow(profile) }

                        item {
                            SectionHeader(
                                title = if (isSelf) "Issues you raised" else "Issues raised",
                                subtitle = if (profile.issuesPosted == 1L) {
                                    "1 issue"
                                } else {
                                    "${profile.issuesPosted} issues"
                                }
                            )
                        }

                        if (state.issues.isEmpty()) {
                            item {
                                Text(
                                    text = if (isSelf) {
                                        "You have not raised anything yet. " +
                                            "The feed's button is waiting."
                                    } else {
                                        "This member has not raised anything yet."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(items = state.issues, key = { it.id }) { issue ->
                            IssueCard(
                                issue = issue,
                                isVoting = issue.id in state.pendingVoteIds,
                                onOpen = { onOpenIssue(issue) },
                                onSupport = { onVote(issue, VoteStance.SUPPORT) },
                                onDecline = { onVote(issue, VoteStance.DECLINE) },
                                onOpenAuthor = {}
                            )
                        }

                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    if (state.isEditing) {
        EditProfileDialog(
            state = state,
            onNameChanged = onEditNameChanged,
            onBioChanged = onEditBioChanged,
            onNeighbourhoodChanged = onEditNeighbourhoodChanged,
            onDismiss = onCancelEditing,
            onSave = onSaveProfile
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = {
                Text(
                    "Your issues, votes and comments all stay where they are. " +
                        "You can sign back in any time."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    }
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Stay signed in") }
            }
        )
    }
}

@Composable
private fun ProfileHeader(profile: PublicProfile) {
    val user = profile.user
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        MemberAvatar(initials = user.initials, colorHex = user.avatarColor, size = 76)

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (user.memberRole != MemberRole.CITIZEN) {
                Spacer(Modifier.width(8.dp))
                OfficialBadge(user.memberRole)
            }
        }

        if (user.neighbourhood.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = user.neighbourhood,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (user.bio.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Member since ${Formatters.monthAndYear(user.joinedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The three numbers that say what this member has actually achieved. */
@Composable
private fun ContributionRow(profile: PublicProfile) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(vertical = 16.dp)) {
            ContributionCell(
                value = profile.issuesPosted.toString(),
                label = "raised",
                icon = Icons.Filled.Edit,
                modifier = Modifier.weight(1f)
            )
            ContributionCell(
                value = Formatters.compactCount(profile.supportReceived),
                label = "support won",
                icon = Icons.Filled.ThumbUp,
                modifier = Modifier.weight(1f)
            )
            ContributionCell(
                value = profile.issuesResolved.toString(),
                label = "resolved",
                icon = Icons.Filled.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ContributionCell(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditProfileDialog(
    state: ProfileUiState,
    onNameChanged: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onNeighbourhoodChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit your profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = state.editName,
                    onValueChange = onNameChanged,
                    label = { Text("Name") },
                    singleLine = true,
                    isError = state.editError != null,
                    supportingText = state.editError?.let { message ->
                        { Text(message) }
                    },
                    enabled = !state.isSavingProfile,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.editNeighbourhood,
                    onValueChange = onNeighbourhoodChanged,
                    label = { Text("Neighbourhood") },
                    singleLine = true,
                    enabled = !state.isSavingProfile,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.editBio,
                    onValueChange = onBioChanged,
                    label = { Text("About you") },
                    placeholder = { Text("A line about what you care about locally") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !state.isSavingProfile,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Changing your name updates it on the issues and comments " +
                        "you have already posted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !state.isSavingProfile) {
                if (state.isSavingProfile) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving")
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !state.isSavingProfile) {
                Text("Cancel")
            }
        }
    )
}
