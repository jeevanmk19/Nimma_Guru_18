package com.nimmaguru.app.feature.profile.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.core.ui.components.GuruAvatar

@Composable
fun MyProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: MyProfileViewModel = hiltViewModel(),
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToGuruDetail: (String) -> Unit = {},
    onSignedOut: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MyProfileEvent.SignedOut -> onSignedOut()
                is MyProfileEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = uiState) {
            is MyProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is MyProfileUiState.NotLoggedIn -> {
                NotLoggedInState(modifier = Modifier.padding(innerPadding))
            }
            is MyProfileUiState.Authenticated -> {
                MyGuruProfileContent(
                    modifier = Modifier.padding(innerPadding),
                    guru = state.guru,
                    onNavigateToOnboarding = onNavigateToOnboarding,
                    onSignOut = viewModel::signOut,
                )
            }
            is MyProfileUiState.Student -> {
                StudentProfileContent(
                    modifier = Modifier.padding(innerPadding),
                    user = state.user,
                    onNavigateToOnboarding = onNavigateToOnboarding,
                    onSignOut = viewModel::signOut,
                )
            }
            is MyProfileUiState.NoProfile -> {
                MyGuruProfileContent(
                    modifier = Modifier.padding(innerPadding),
                    guru = null,
                    onNavigateToOnboarding = onNavigateToOnboarding,
                    onSignOut = viewModel::signOut,
                )
            }
        }
    }
}

@Composable
fun StudentProfileContent(
    user: com.nimmaguru.app.core.model.User,
    modifier: Modifier = Modifier,
    onNavigateToOnboarding: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = user.name.take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = user.name, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.district_label_display, user.district),
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.become_guru_promo_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.become_guru_promo_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.become_guru_button))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(stringResource(R.string.sign_out_button))
        }
    }
}

@Composable
private fun NotLoggedInState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "👋",
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.login_prompt),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MyGuruProfileContent(
    modifier: Modifier = Modifier,
    guru: Guru?,
    onNavigateToOnboarding: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GuruAvatar(
            name = guru?.nameEn ?: stringResource(R.string.default_guru_name),
            photoUrl = guru?.photoUrl ?: "",
            size = 120.dp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = guru?.nameEn?.takeIf { it.isNotBlank() } ?: stringResource(R.string.my_profile_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        if (guru != null) {
            Text(
                text = "${guru.village}, ${guru.district}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.my_profile_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn(
                    label = stringResource(R.string.sessions_label),
                    value = guru?.totalSessions?.toString() ?: "0",
                )
                StatColumn(
                    label = stringResource(R.string.skills_section),
                    value = guru?.skills?.size?.toString() ?: "0",
                )
                StatColumn(
                    label = stringResource(R.string.rating_label),
                    value = guru?.avgRating?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—",
                )
            }
        }

        if (guru?.bioEn?.isNotBlank() == true) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = guru.bioEn,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateToOnboarding,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = if (guru == null) stringResource(R.string.become_guru_button)
                else stringResource(R.string.edit_profile_button),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.sign_out_button),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
