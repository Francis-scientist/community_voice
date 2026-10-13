package com.example.communityvoice.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The first thing a new person sees: what the app is for, then two buttons.
 *
 * There is a third way in - "Look around first" - because somebody deciding
 * whether to join should be able to read the feed before handing over an email
 * address. The backend allows that too: the list and comment endpoints work
 * without a token, only voting and posting need one.
 */
@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    onBrowse: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(22.dp)
                        .size(44.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Community Voice",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Raise what matters where you live, and let your " +
                    "neighbours back it or push back.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(36.dp))

            SellingPoint(
                icon = Icons.Filled.Campaign,
                title = "Raise an issue",
                body = "Potholes, a broken pump, an unlit street. Say what it is and who it affects."
            )
            SellingPoint(
                icon = Icons.Filled.HowToVote,
                title = "Support or decline",
                body = "One vote each, and you can change your mind. Disagreement counts too."
            )
            SellingPoint(
                icon = Icons.Filled.TrackChanges,
                title = "Follow it through",
                body = "Officials move issues from Open to Resolved, with a note at every step."
            )
            SellingPoint(
                icon = Icons.Filled.Insights,
                title = "See the picture",
                body = "Which categories dominate, what gets fixed, and who is speaking up."
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Create an account")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("I already have an account")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onBrowse) {
                Text("Look around first")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SellingPoint(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
