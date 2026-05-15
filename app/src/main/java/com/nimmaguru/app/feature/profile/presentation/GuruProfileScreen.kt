package com.nimmaguru.app.feature.profile.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.core.ui.components.GuruAvatar
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme
import java.util.Locale

@Composable
fun GuruProfileScreen(
    guruId: String,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToSession: (String, String) -> Unit = { _, _ -> },
    onNavigateToAppreciation: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val isKn = LocalConfiguration.current.locales[0].language == "kn"

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShareProfile -> {
                    val guru = event.guru
                    val name = guru.nameEn.ifBlank { guru.nameKn }
                    val skills = guru.skills.joinToString(", ").ifBlank { "various subjects" }
                    val text = context.getString(R.string.profile_share_message, name, skills, guru.id)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
                is ProfileEvent.NavigateToSession ->
                    onNavigateToSession(event.sessionId, event.guruId)
                is ProfileEvent.NavigateToAppreciation ->
                    onNavigateToAppreciation(event.guruId)
                is ProfileEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is ProfileUiState.Success -> {
                GuruProfileContent(
                    modifier = Modifier.padding(innerPadding),
                    guru = state.guru,
                    sessions = state.sessions,
                    currentUserId = state.currentUserId,
                    isKn = isKn,
                    onAction = viewModel::onAction,
                )
            }
            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(state.messageRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.onAction(ProfileAction.Retry) }) {
                            Text(stringResource(R.string.retry_button))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuruProfileContent(
    modifier: Modifier = Modifier,
    guru: Guru = Guru(),
    sessions: List<Session> = emptyList(),
    currentUserId: String? = null,
    isKn: Boolean = false,
    onAction: (ProfileAction) -> Unit = {},
) {
    val displayName = if (isKn && guru.nameKn.isNotBlank()) guru.nameKn
                      else guru.nameEn.ifBlank { stringResource(R.string.default_guru_name) }
    val displayBio = if (isKn && guru.bioKn.isNotBlank()) guru.bioKn else guru.bioEn

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GuruAvatar(
            name = guru.nameEn.ifBlank { stringResource(R.string.default_guru_name) },
            photoUrl = guru.photoUrl,
            size = 120.dp,
            contentDescription = stringResource(R.string.cd_guru_photo),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Show the other-language name as a secondary line if available
        if (isKn && guru.nameEn.isNotBlank()) {
            Text(
                text = guru.nameEn,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (!isKn && guru.nameKn.isNotBlank()) {
            Text(
                text = guru.nameKn,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = stringResource(R.string.cd_location_icon),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = buildString {
                    append(guru.village)
                    if (guru.district.isNotBlank()) append(", ${guru.district}")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.cd_star_rating),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", guru.avgRating),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.reviews_count, guru.appreciationCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "${guru.totalSessions} ${stringResource(R.string.sessions_label)}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        IconButton(
            onClick = { onAction(ProfileAction.Share) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.cd_share_button),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        if (displayBio.isNotBlank()) {
            SectionHeader(title = stringResource(R.string.about_section))
            Text(
                text = displayBio,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (guru.skills.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.skills_section))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                guru.skills.forEach { skill ->
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = skill, // skill keys are already English; matches both locales' chip set
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (sessions.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.upcoming_sessions_section))
            sessions.forEach { session ->
                SessionItem(
                    session = session,
                    currentUserId = currentUserId,
                    onRsvp = { onAction(ProfileAction.AttendSession(session.id)) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (guru.availability.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.available_section))
            guru.availability.forEach { (day, time) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = day, style = MaterialTheme.typography.bodyLarge)
                        Text(text = time, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { onAction(ProfileAction.PostAppreciation) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.post_thank_you),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SessionItem(
    session: Session,
    currentUserId: String?,
    onRsvp: () -> Unit,
) {
    val isJoined = currentUserId != null && currentUserId in session.attendees
    val isFull = session.attendees.size >= session.maxStudents && !isJoined

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.subject,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${session.venue} · ${session.startTime} - ${session.endTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.seats_info, session.attendees.size, session.maxStudents),
                    style = MaterialTheme.typography.labelLarge,
                )

                when {
                    isJoined -> {
                        OutlinedButton(
                            onClick = onRsvp,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(stringResource(R.string.joined_status))
                        }
                    }
                    isFull -> {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(stringResource(R.string.full_label))
                        }
                    }
                    else -> {
                        Button(
                            onClick = onRsvp,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(stringResource(R.string.attend_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview_GuruProfile() {
    NimmaGuruTheme {
        GuruProfileContent(
            guru = Guru(
                nameEn = "Ramesh Kumar",
                nameKn = "ರಮೇಶ್ ಕುಮಾರ್",
                village = "Hunsur",
                district = "Mysuru",
                skills = listOf("Math", "Science", "Chess"),
                availability = mapOf("Sat" to "10:00-12:00", "Sun" to "14:00-16:00"),
                bioEn = "Retired math teacher with 30 years experience...",
                avgRating = 4.5f,
                appreciationCount = 12,
                totalSessions = 45,
            ),
        )
    }
}
