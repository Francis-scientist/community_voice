package com.example.communityvoice.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.communityvoice.data.Issue
import com.example.communityvoice.data.IssueCategory
import com.example.communityvoice.data.Member
import com.example.communityvoice.data.MemberRole
import com.example.communityvoice.ui.auth.AuthViewModel
import com.example.communityvoice.ui.auth.LoginScreen
import com.example.communityvoice.ui.auth.RegisterScreen
import com.example.communityvoice.ui.auth.SessionViewModel
import com.example.communityvoice.ui.auth.WelcomeScreen
import com.example.communityvoice.ui.create.CreateIssueScreen
import com.example.communityvoice.ui.create.CreateIssueViewModel
import com.example.communityvoice.ui.detail.IssueDetailScreen
import com.example.communityvoice.ui.detail.IssueDetailViewModel
import com.example.communityvoice.ui.feed.FeedScreen
import com.example.communityvoice.ui.feed.FeedViewModel
import com.example.communityvoice.ui.insights.InsightsScreen
import com.example.communityvoice.ui.insights.InsightsViewModel
import com.example.communityvoice.ui.profile.ProfileScreen
import com.example.communityvoice.ui.profile.ProfileViewModel

/**
 * Every route in the app, in one place, so a typo is a compile error rather
 * than a navigation that silently does nothing.
 *
 * The two parameterised routes keep their pattern (`issue/{issueId}`) for
 * registering the destination, and a function for building a concrete path.
 */
private object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val FEED = "feed"
    const val INSIGHTS = "insights"
    const val CREATE = "create"

    const val ISSUE_PATTERN = "issue/{issueId}"
    fun issue(issueId: String) = "issue/$issueId"

    const val PROFILE_PATTERN = "profile/{userId}"
    fun profile(userId: String) = "profile/$userId"
}

/**
 * One tab in the bottom bar.
 *
 * [pattern] is what the navigation graph registered, and so what a current
 * destination is compared against; [target] is the concrete path to navigate
 * to. They differ for the profile tab, whose path contains a user id.
 */
private data class BottomTab(
    val pattern: String,
    val target: String,
    val label: String,
    val icon: ImageVector
)

/**
 * The whole app.
 *
 * Two things are worth noticing about the shape of this:
 *
 *  1. **Signing out is not a navigation.** [SessionViewModel] exposes the
 *     session as a flow and this composable reads it. When a token expires
 *     mid-request, ApiClient clears the session, this recomposes, and the
 *     welcome screen appears - from wherever the user happened to be, with no
 *     individual screen having to handle it.
 *
 *  2. **The feed's ViewModel lives up here**, not inside the feed screen, so
 *     posting an issue or voting on the detail screen can hand the new numbers
 *     straight back to the list instead of forcing a reload.
 */
@Composable
fun CommunityVoiceApp(
    sessionViewModel: SessionViewModel = viewModel(),
    feedViewModel: FeedViewModel = viewModel()
) {
    val navController = rememberNavController()
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val member: Member? = session?.member
    val isSignedIn = member != null

    /**
     * Sends a visitor to sign up at the moment they try to do something that
     * needs an account - a better prompt than a wall in front of the whole app.
     */
    val requireSignIn: () -> Unit = { navController.navigate(Routes.REGISTER) }

    val openIssue: (Issue) -> Unit = { issue ->
        if (issue.id.isNotBlank()) navController.navigate(Routes.issue(issue.id))
    }

    val openProfile: (String) -> Unit = { userId ->
        if (userId.isNotBlank()) navController.navigate(Routes.profile(userId))
    }

    Scaffold(
        // Each screen below has its own Scaffold, which applies the status-bar
        // inset for its top app bar. Zeroing the insets here stops that inset
        // being counted twice and leaving a gap above every app bar - this
        // outer Scaffold exists only to hold the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { BottomBar(navController = navController, myUserId = member?.id) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // A visitor with no session starts at the welcome screen; somebody
            // already signed in goes straight to the feed.
            startDestination = if (isSignedIn) Routes.FEED else Routes.WELCOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // --------------------------------------------------- getting in

            composable(Routes.WELCOME) {
                WelcomeScreen(
                    onSignIn = { navController.navigate(Routes.LOGIN) },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    onBrowse = { navController.navigate(Routes.FEED) }
                )
            }

            composable(Routes.LOGIN) {
                val authViewModel: AuthViewModel = viewModel()
                val state by authViewModel.state.collectAsStateWithLifecycle()

                LoginScreen(
                    state = state,
                    onEmailChanged = authViewModel::onEmailChanged,
                    onPasswordChanged = authViewModel::onPasswordChanged,
                    onTogglePasswordVisible = authViewModel::togglePasswordVisible,
                    onSubmit = {
                        authViewModel.login {
                            feedViewModel.load()
                            navController.goToFeedAfterAuth()
                        }
                    },
                    onGoToRegister = {
                        authViewModel.reset()
                        navController.navigate(Routes.REGISTER)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.REGISTER) {
                val authViewModel: AuthViewModel = viewModel()
                val state by authViewModel.state.collectAsStateWithLifecycle()

                RegisterScreen(
                    state = state,
                    onDisplayNameChanged = authViewModel::onDisplayNameChanged,
                    onEmailChanged = authViewModel::onEmailChanged,
                    onPasswordChanged = authViewModel::onPasswordChanged,
                    onNeighbourhoodChanged = authViewModel::onNeighbourhoodChanged,
                    onTogglePasswordVisible = authViewModel::togglePasswordVisible,
                    onSubmit = {
                        authViewModel.register {
                            feedViewModel.load()
                            navController.goToFeedAfterAuth()
                        }
                    },
                    onGoToLogin = {
                        authViewModel.reset()
                        navController.navigate(Routes.LOGIN)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // --------------------------------------------------------- feed

            composable(Routes.FEED) {
                val state by feedViewModel.state.collectAsStateWithLifecycle()

                FeedScreen(
                    state = state,
                    isSignedIn = isSignedIn,
                    onRefresh = feedViewModel::refresh,
                    onLoadMore = feedViewModel::loadMore,
                    onSearchChanged = feedViewModel::onSearchChanged,
                    onClearSearch = feedViewModel::clearSearch,
                    onCategorySelected = feedViewModel::onCategorySelected,
                    onStatusSelected = feedViewModel::onStatusSelected,
                    onSortSelected = feedViewModel::onSortSelected,
                    onScopeSelected = feedViewModel::onScopeSelected,
                    onToggleFilters = feedViewModel::toggleFilters,
                    onClearFilters = feedViewModel::clearFilters,
                    onVote = feedViewModel::vote,
                    onOpenIssue = openIssue,
                    onOpenAuthor = openProfile,
                    onAddIssue = { navController.navigate(Routes.CREATE) },
                    onRequireSignIn = requireSignIn,
                    onUserMessageShown = feedViewModel::onUserMessageShown
                )
            }

            // ----------------------------------------------------- insights

            composable(Routes.INSIGHTS) {
                val insightsViewModel: InsightsViewModel = viewModel()
                val state by insightsViewModel.state.collectAsStateWithLifecycle()

                InsightsScreen(
                    state = state,
                    onRefresh = insightsViewModel::refresh,
                    onRetry = insightsViewModel::load,
                    onOpenCategory = { category: IssueCategory ->
                        // Tapping a bar filters the feed and takes you there,
                        // which is the useful thing to do with the number.
                        feedViewModel.clearFilters()
                        feedViewModel.onCategorySelected(category)
                        navController.navigate(Routes.FEED) {
                            popUpTo(Routes.FEED) { inclusive = true }
                        }
                    },
                    onOpenMember = openProfile
                )
            }

            // ----------------------------------------------- raise an issue

            composable(Routes.CREATE) {
                val createViewModel: CreateIssueViewModel = viewModel()
                val state by createViewModel.state.collectAsStateWithLifecycle()

                if (member == null) {
                    // Should not be reachable without a session, but if the
                    // token expires while the form is open, ask for a new one
                    // rather than crash on a null author.
                    WelcomeScreen(
                        onSignIn = { navController.navigate(Routes.LOGIN) },
                        onRegister = { navController.navigate(Routes.REGISTER) },
                        onBrowse = { navController.popBackStack() }
                    )
                } else {
                    CreateIssueScreen(
                        state = state,
                        author = member,
                        onTitleChanged = createViewModel::onTitleChanged,
                        onDescriptionChanged = createViewModel::onDescriptionChanged,
                        onLocationChanged = createViewModel::onLocationChanged,
                        onCategorySelected = createViewModel::onCategorySelected,
                        onTogglePreview = createViewModel::togglePreview,
                        onSubmit = {
                            createViewModel.submit { created ->
                                feedViewModel.load()
                                feedViewModel.showMessage("Issue posted. Thank you!")
                                // Straight into the new issue, replacing the
                                // form on the back stack so back goes to feed.
                                navController.popBackStack()
                                navController.navigate(Routes.issue(created.id))
                            }
                        },
                        onBack = {
                            createViewModel.reset()
                            navController.popBackStack()
                        }
                    )
                }
            }

            // -------------------------------------------------- one issue

            composable(Routes.ISSUE_PATTERN) { backStackEntry ->
                val detailViewModel: IssueDetailViewModel = viewModel()
                val state by detailViewModel.state.collectAsStateWithLifecycle()

                val issueId = backStackEntry.arguments
                    ?.getString(IssueDetailViewModel.ARG_ISSUE_ID)
                    .orEmpty()

                /**
                 * Leaving hands the newest numbers back to the feed, so the
                 * card you came from shows the vote you just cast - and drops
                 * the row entirely if the issue was deleted.
                 */
                val leave: () -> Unit = {
                    if (state.wasDeleted) {
                        feedViewModel.removeIssue(issueId)
                    } else {
                        detailViewModel.latestIssue?.let(feedViewModel::applyUpdatedIssue)
                    }
                    navController.popBackStack()
                }

                IssueDetailScreen(
                    state = state,
                    isSignedIn = isSignedIn,
                    viewerRole = member?.memberRole ?: MemberRole.CITIZEN,
                    onBack = leave,
                    onRetry = detailViewModel::load,
                    onVote = detailViewModel::vote,
                    onCommentDraftChanged = detailViewModel::onCommentDraftChanged,
                    onPostComment = detailViewModel::postComment,
                    onDeleteComment = detailViewModel::deleteComment,
                    onLoadMoreComments = { detailViewModel.loadComments(reset = false) },
                    onUpdateStatus = detailViewModel::updateStatus,
                    onDeleteIssue = detailViewModel::deleteIssue,
                    onOpenAuthor = openProfile,
                    onShare = { issue -> shareIssue(context, issue) },
                    onRequireSignIn = requireSignIn,
                    onUserMessageShown = detailViewModel::onUserMessageShown
                )
            }

            // ---------------------------------------------------- a profile

            composable(Routes.PROFILE_PATTERN) { backStackEntry ->
                val profileViewModel: ProfileViewModel = viewModel()
                val state by profileViewModel.state.collectAsStateWithLifecycle()

                val viewedUserId = backStackEntry.arguments
                    ?.getString(ProfileViewModel.ARG_USER_ID)
                    .orEmpty()
                val isSelf = member != null && member.id == viewedUserId

                ProfileScreen(
                    state = state,
                    isSelf = isSelf,
                    showBackButton = navController.previousBackStackEntry != null,
                    onBack = { navController.popBackStack() },
                    onRetry = profileViewModel::load,
                    onStartEditing = profileViewModel::startEditing,
                    onCancelEditing = profileViewModel::cancelEditing,
                    onEditNameChanged = profileViewModel::onEditNameChanged,
                    onEditBioChanged = profileViewModel::onEditBioChanged,
                    onEditNeighbourhoodChanged = profileViewModel::onEditNeighbourhoodChanged,
                    onSaveProfile = profileViewModel::saveProfile,
                    onSignOut = {
                        sessionViewModel.signOut()
                        // Clearing the session is what actually changes the
                        // app's state; this just empties the back stack so
                        // nothing signed-in is left behind it.
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    },
                    onOpenIssue = openIssue,
                    onVote = { issue, stance ->
                        if (isSignedIn) profileViewModel.vote(issue, stance) else requireSignIn()
                    },
                    onUserMessageShown = profileViewModel::onUserMessageShown
                )
            }
        }
    }
}

/**
 * After signing in or registering, land on the feed and drop the auth screens
 * from the back stack - pressing back from the feed should leave the app, not
 * return to a login form the user has already completed.
 */
private fun NavHostController.goToFeedAfterAuth() {
    navigate(Routes.FEED) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}

/** The bottom bar, hidden on the screens where it would be in the way. */
@Composable
private fun BottomBar(navController: NavHostController, myUserId: String?) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Welcome, the auth forms, the new-issue form and one issue in full are
    // all whole tasks; a tab bar under them invites you to abandon what you
    // are in the middle of.
    val hideOn = setOf(
        Routes.WELCOME,
        Routes.LOGIN,
        Routes.REGISTER,
        Routes.CREATE,
        Routes.ISSUE_PATTERN
    )
    if (currentRoute == null || currentRoute in hideOn) return

    val tabs = buildList {
        add(BottomTab(Routes.FEED, Routes.FEED, "Issues", Icons.Outlined.Forum))
        add(BottomTab(Routes.INSIGHTS, Routes.INSIGHTS, "Insights", Icons.Filled.Insights))
        // The profile tab only means anything once there is an account.
        if (!myUserId.isNullOrBlank()) {
            add(
                BottomTab(
                    pattern = Routes.PROFILE_PATTERN,
                    target = Routes.profile(myUserId),
                    label = "You",
                    icon = Icons.Filled.Person
                )
            )
        }
    }

    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                // Compared against the registered pattern, not the concrete
                // path - "profile/{userId}" is what the destination is called.
                selected = currentRoute == tab.pattern,
                onClick = {
                    navController.navigate(tab.target) {
                        // One entry per tab, rather than a pile of feeds every
                        // time somebody taps between them.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) }
            )
        }
    }
}

/**
 * Hands the issue to whatever the phone can share with - a message, an email,
 * a group chat. Getting a neighbour to come and look is most of what makes an
 * issue move, so this is a first-class action rather than an afterthought.
 */
private fun shareIssue(context: Context, issue: Issue) {
    val text = buildString {
        append(issue.title)
        appendLine()
        appendLine()
        append(issue.description)
        appendLine()
        appendLine()
        append("${issue.supportCount} people support this")
        if (issue.declineCount > 0) append(", ${issue.declineCount} declined")
        append(" on Community Voice.")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, issue.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share this issue"))
}
