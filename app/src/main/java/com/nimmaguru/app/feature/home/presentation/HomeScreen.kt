package com.nimmaguru.app.feature.home.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import com.nimmaguru.app.core.model.AppreciationNote
import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.core.ui.components.GuruAvatar
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToGuruDetail: (String) -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToSession: (String, String) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is HomeUiState.Success -> {
            HomeContent(
                modifier = modifier,
                topGurus = state.topGurus,
                upcomingSessions = state.upcomingSessions,
                joinedSessions = state.joinedSessions,
                recentAppreciations = state.recentAppreciations,
                showOnboardingPrompt = state.showOnboardingPrompt,
                onNavigateToGuruDetail = onNavigateToGuruDetail,
                onNavigateToOnboarding = onNavigateToOnboarding,
                onNavigateToSession = onNavigateToSession,
            )
        }
        is HomeUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(state.messageRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.onAction(HomeAction.Retry) }) {
                        Text(stringResource(R.string.retry_button))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    topGurus: List<Guru> = emptyList(),
    upcomingSessions: List<Session> = emptyList(),
    joinedSessions: List<Session> = emptyList(),
    recentAppreciations: List<AppreciationNote> = emptyList(),
    showOnboardingPrompt: Boolean = false,
    onNavigateToGuruDetail: (String) -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToSession: (String, String) -> Unit = { _, _ -> },
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
    ) {
        // Tagline
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (showOnboardingPrompt) {
            Spacer(modifier = Modifier.height(16.dp))
            OnboardingBanner(onNavigateToOnboarding = onNavigateToOnboarding)
        }

        if (joinedSessions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            HomeSectionHeader(title = stringResource(R.string.my_upcoming_classes), emoji = "📅")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                joinedSessions.forEach { session ->
                    JoinedSessionCard(
                        session = session,
                        onClick = { onNavigateToSession(session.id, session.guruId) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Wall of Fame
        HomeSectionHeader(title = stringResource(R.string.wall_of_fame_title), emoji = "🏆")

        if (topGurus.isEmpty()) {
            Text(
                text = stringResource(R.string.home_no_gurus),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(topGurus, key = { it.id }) { guru ->
                    WallOfFameCard(
                        guru = guru,
                        onClick = { onNavigateToGuruDetail(guru.id) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upcoming Sessions
        HomeSectionHeader(title = stringResource(R.string.upcoming_sessions_title), emoji = "🗓️")

        if (upcomingSessions.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.home_no_sessions_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.home_no_sessions_cta),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                upcomingSessions.take(3).forEach { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = session.subject, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = formatDateTime(session),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Appreciations
        HomeSectionHeader(title = stringResource(R.string.recent_appreciations_title), emoji = "🙏")

        if (recentAppreciations.isEmpty()) {
            Text(
                text = stringResource(R.string.home_no_appreciations),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recentAppreciations.take(3).forEach { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.home_appreciation_format, note.message),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.home_appreciation_rating, note.rating),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun formatDateTime(session: Session): String {
    val locale = Locale.getDefault()
    val dateFormat = remember(locale) { SimpleDateFormat("EEE, dd MMM", locale) }
    val date = if (session.date > 0) dateFormat.format(Date(session.date)) else ""
    val time = if (session.startTime.isNotBlank() && session.endTime.isNotBlank()) {
        "${session.startTime} - ${session.endTime}"
    } else {
        session.startTime
    }
    return when {
        date.isNotBlank() && time.isNotBlank() -> stringResource(R.string.home_session_date_time, date, time)
        date.isNotBlank() -> date
        else -> time
    }
}

@Composable
private fun OnboardingBanner(onNavigateToOnboarding: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.become_guru_promo_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.become_guru_promo_desc),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onNavigateToOnboarding) {
                Text(stringResource(R.string.home_join_now))
            }
        }
    }
}

@Composable
private fun JoinedSessionCard(
    session: Session,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = session.subject,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "${session.guruNameEn} · ${session.venue}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Text(
                text = stringResource(R.string.joined_status),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun HomeSectionHeader(title: String, emoji: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun WallOfFameCard(
    guru: Guru,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GuruAvatar(
                name = guru.nameEn,
                photoUrl = guru.photoUrl,
                size = 72.dp,
                contentDescription = stringResource(R.string.cd_guru_photo),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = guru.nameEn,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = guru.village,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", guru.avgRating),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = " · ${guru.totalSessions} ${stringResource(R.string.sessions_label)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_HomeScreen() {
    NimmaGuruTheme {
        HomeContent(
            topGurus = listOf(
                Guru(
                    id = "1",
                    nameEn = "Ramesh Kumar",
                    village = "Hunsur",
                    skills = listOf("Math", "Science"),
                    avgRating = 4.8f,
                    totalSessions = 45,
                    appreciationCount = 28,
                ),
            ),
        )
    }
}
