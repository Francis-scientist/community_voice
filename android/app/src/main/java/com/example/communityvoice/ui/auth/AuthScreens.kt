package com.example.communityvoice.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.communityvoice.ui.theme.CommunityVoiceTheme

/**
 * Sign in. Deliberately short: email, password, one button.
 *
 * Both fields are checked locally first so a typo gets an instant answer, then
 * again by the backend, which is the only thing that can actually confirm a
 * password.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: AuthFormState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
    onGoToRegister: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sign in to support issues and raise your own.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            EmailField(
                value = state.email,
                error = state.emailError,
                enabled = !state.isSubmitting,
                imeAction = ImeAction.Next,
                onValueChange = onEmailChanged
            )

            Spacer(Modifier.height(12.dp))

            PasswordField(
                value = state.password,
                error = state.passwordError,
                visible = state.passwordVisible,
                enabled = !state.isSubmitting,
                supportingText = null,
                onValueChange = onPasswordChanged,
                onToggleVisible = onTogglePasswordVisible,
                onDone = onSubmit
            )

            FormErrorBanner(state.formError)

            Spacer(Modifier.height(24.dp))

            SubmitButton(
                label = "Sign in",
                busyLabel = "Signing in...",
                isSubmitting = state.isSubmitting,
                onClick = onSubmit
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New here?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onGoToRegister, enabled = !state.isSubmitting) {
                    Text("Create an account")
                }
            }
        }
    }
}

/**
 * Sign up. Name, email, password, and an optional neighbourhood.
 *
 * The neighbourhood is optional on purpose - asking for less at the start gets
 * more people through the form, and it can be filled in later from the profile
 * screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    state: AuthFormState,
    onDisplayNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNeighbourhoodChanged: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
    onGoToLogin: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create account") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Join your community",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your name is shown on the issues you raise, so neighbours " +
                    "know who is speaking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChanged,
                label = { Text("Your name") },
                placeholder = { Text("e.g. Amina Yusuf") },
                leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                singleLine = true,
                isError = state.displayNameError != null,
                supportingText = {
                    Text(state.displayNameError ?: "Shown publicly on your issues")
                },
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            EmailField(
                value = state.email,
                error = state.emailError,
                enabled = !state.isSubmitting,
                imeAction = ImeAction.Next,
                onValueChange = onEmailChanged
            )

            Spacer(Modifier.height(12.dp))

            PasswordField(
                value = state.password,
                error = state.passwordError,
                visible = state.passwordVisible,
                enabled = !state.isSubmitting,
                supportingText = "At least 8 characters, with a letter and a number",
                onValueChange = onPasswordChanged,
                onToggleVisible = onTogglePasswordVisible,
                onDone = null
            )

            // Only appears once there is something to measure, so an empty
            // form does not open with a red "too short" warning.
            AnimatedVisibility(visible = state.password.isNotEmpty()) {
                PasswordStrengthMeter(state.passwordStrength)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.neighbourhood,
                onValueChange = onNeighbourhoodChanged,
                label = { Text("Neighbourhood (optional)") },
                placeholder = { Text("e.g. Market Road") },
                leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
                singleLine = true,
                supportingText = { Text("Helps neighbours recognise local issues") },
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            FormErrorBanner(state.formError)

            Spacer(Modifier.height(24.dp))

            SubmitButton(
                label = "Create account",
                busyLabel = "Creating...",
                isSubmitting = state.isSubmitting,
                onClick = onSubmit
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already a member?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onGoToLogin, enabled = !state.isSubmitting) {
                    Text("Sign in")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ------------------------------------------------------------ shared pieces

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@Composable
private fun EmailField(
    value: String,
    error: String?,
    enabled: Boolean,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email") },
        placeholder = { Text("you@example.com") },
        leadingIcon = { Icon(Icons.Filled.MailOutline, contentDescription = null) },
        singleLine = true,
        isError = error != null,
        // Left null when there is no message, so the field does not reserve a
        // blank line of space under itself.
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordField(
    value: String,
    error: String?,
    visible: Boolean,
    enabled: Boolean,
    supportingText: String?,
    onValueChange: (String) -> Unit,
    onToggleVisible: () -> Unit,
    onDone: (() -> Unit)?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        singleLine = true,
        isError = error != null,
        // The error wins over the standing hint when there is one.
        supportingText = (error ?: supportingText).let { message ->
            if (message != null) {
                { Text(message) }
            } else {
                null
            }
        },
        enabled = enabled,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next
        ),
        // Lets the keyboard's "Done" key submit the form, which is how most
        // people finish a two-field sign-in.
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * The strength meter under the password box.
 *
 * It only ever encourages - the one rule that blocks submission is the
 * backend's, and it is stated in the field's own helper text.
 */
@Composable
private fun PasswordStrengthMeter(strength: PasswordStrength) {
    val accents = CommunityVoiceTheme.accents
    val color: Color = when (strength) {
        PasswordStrength.TOO_SHORT -> MaterialTheme.colorScheme.error
        PasswordStrength.WEAK -> accents.statusReview
        PasswordStrength.FAIR -> accents.statusProgress
        PasswordStrength.STRONG -> accents.support.tint
    }
    val fraction by animateFloatAsState(strength.fraction, label = "passwordStrength")

    Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)) {
        LinearProgressIndicator(
            progress = { fraction },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Password strength: ${strength.label}",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/** A problem that belongs to the whole form rather than one field. */
@Composable
private fun FormErrorBanner(message: String?) {
    AnimatedVisibility(visible = message != null) {
        Column {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SubmitButton(
    label: String,
    busyLabel: String,
    isSubmitting: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(12.dp))
            Text(busyLabel)
        } else {
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}
