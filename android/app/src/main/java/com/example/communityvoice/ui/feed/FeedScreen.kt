package com.example.communityvoice.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.communityvoice.data.FeedSort
import com.example.communityvoice.data.Formatters
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.IssueCategory
import com.example.communityvoice.data.IssueStatus
import com.example.communityvoice.data.VoteStance
import com.example.communityvoice.ui.components.EmptyState
import com.example.communityvoice.ui.components.ErrorState
import com.example.communityvoice.ui.components.LoadingState

/**
 * The main feed: search, filters, and the list of issues.
 *
 * Everything on this screen works without an account - the counts, the search,
 * the comments. Only voting and posting need a session, and [onRequireSignIn]
 * is what nudges a visitor towards one at the moment they try.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedUiState,
    isSignedIn: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCategorySelected: (IssueCategory?) -> Unit,
    onStatusSelected: (IssueStatus?) -> Unit,
    onSortSelected: (FeedSort) -> Unit,
    onScopeSelected: (FeedScope) -> Unit,
    onToggleFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onVote: (Issue, VoteStance) -> Unit,
    onOpenIssue: (Issue) -> Unit,
    onOpenAuthor: (String) -> Unit,
    onAddIssue: () -> Unit,
    onRequireSignIn: () -> Unit,
    onUserMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.userMessage) {
        val message = state.userMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onUserMessageShown()
        }
    }

    // Fetches the next page when the last few cards come into view, so the
    // list keeps going without a "load more" button.
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }
            .collect { near -> if (near) onLoadMore() }
    }

    /** Voting needs an account; tapping while signed out offers to make one. */
    val guardedVote: (Issue, VoteStance) -> Unit = { issue, stance ->
        if (isSignedIn) onVote(issue, stance) else onRequireSignIn()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Voice") },
                actions = {
                    IconButton(onClick = onToggleFilters) {
                        Icon(
                            if (state.filtersExpanded) Icons.Filled.Close else Icons.Filled.Tune,
                            contentDescription = if (state.filtersExpanded) {
                                "Hide filters"
                            } else {
                                "Show filters"
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (isSignedIn) onAddIssue() else onRequireSignIn() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Raise an issue") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchBar(
                value = state.search,
                onValueChange = onSearchChanged,
                onClear = onClearSearch
            )

            SortChipRow(selected = state.sort, onSelected = onSortSelected)

            AnimatedVisibility(visible = state.filtersExpanded) {
                FilterPanel(
                    state = state,
                    isSignedIn = isSignedIn,
                    onCategorySelected = onCategorySelected,
                    onStatusSelected = onStatusSelected,
                    onScopeSelected = onScopeSelected,
                    onClearFilters = onClearFilters
                )
            }

            ResultSummary(state = state, onClearFilters = onClearFilters)

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && state.issues.isEmpty() ->
                        LoadingState("Loading issues...")

                    state.errorMessage != null && state.issues.isEmpty() ->
                        ErrorState(
                            title = "Could not load issues",
                            message = state.errorMessage,
                            onRetry = onRefresh
                        )

                    state.isEmpty && state.hasActiveFilters ->
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = "Nothing matches",
                            message = "No issues match these filters. Try widening the search.",
                            actionLabel = "Clear filters",
                            onAction = onClearFilters
                        )

                    state.isEmpty ->
                        EmptyState(
                            icon = Icons.Filled.Campaign,
                            title = "No issues yet",
                            message = "Be the first to raise something that matters " +
                                "in your community.",
                            actionLabel = "Raise the first issue",
                            onAction = { if (isSignedIn) onAddIssue() else onRequireSignIn() }
                        )

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        // Extra bottom padding so the last card clears the FAB.
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = state.issues, key = { it.id }) { issue ->
                            IssueCard(
                                issue = issue,
                                isVoting = issue.id in state.pendingVoteIds,
                                onOpen = { onOpenIssue(issue) },
                                onSupport = { guardedVote(issue, VoteStance.SUPPORT) },
                                onDecline = { guardedVote(issue, VoteStance.DECLINE) },
                                onOpenAuthor = { onOpenAuthor(issue.author.id) }
                            )
                        }
                        item { LoadingMoreRow(visible = state.isLoadingMore) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search issues, places, people") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/** The always-visible ordering row - the one filter people reach for most. */
@Composable
private fun SortChipRow(selected: FeedSort, onSelected: (FeedSort) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FeedSort.entries) { sort ->
            FilterChip(
                selected = sort == selected,
                onClick = { onSelected(sort) },
                label = { Text(sort.label) }
            )
        }
    }
}

/** Category, status and scope - hidden behind the tune button by default. */
@Composable
private fun FilterPanel(
    state: FeedUiState,
    isSignedIn: Boolean,
    onCategorySelected: (IssueCategory?) -> Unit,
    onStatusSelected: (IssueStatus?) -> Unit,
    onScopeSelected: (FeedScope) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        FilterLabel("Category")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(IssueCategory.entries) { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.label) },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        FilterLabel("Progress")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(IssueStatus.entries) { status ->
                FilterChip(
                    selected = state.status == status,
                    onClick = { onStatusSelected(status) },
                    label = { Text(status.label) }
                )
            }
        }

        // Only useful once there is an account to scope "mine" to.
        if (isSignedIn) {
            Spacer(Modifier.height(10.dp))
            FilterLabel("Show")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FeedScope.entries) { scope ->
                    FilterChip(
                        selected = state.scope == scope,
                        onClick = { onScopeSelected(scope) },
                        label = { Text(scope.label) }
                    )
                }
            }
        }

        if (state.hasActiveFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClearFilters) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Clear all filters")
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/** "24 issues · Trending" - confirms the filters did what you expected. */
@Composable
private fun ResultSummary(state: FeedUiState, onClearFilters: () -> Unit) {
    if (state.issues.isEmpty()) {
        Spacer(Modifier.height(4.dp))
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = buildString {
                append(Formatters.compactCount(state.totalItems))
                append(if (state.totalItems == 1L) " issue" else " issues")
                append(" · ")
                append(state.sort.label)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.hasActiveFilters && !state.filtersExpanded) {
            TextButton(onClick = onClearFilters) { Text("Clear") }
        }
    }
}
