package com.nimmaguru.app.feature.calendar.presentation

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    guruId: String,
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onNavigateToGuru: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SessionDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.session_detail_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_navigation_desc),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = uiState) {
            is SessionDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is SessionDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(state.messageRes),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.onAction(SessionDetailAction.Retry) }) {
                            Text(stringResource(R.string.retry_button))
                        }
                    }
                }
            }
            is SessionDetailUiState.Success -> {
                SessionDetailContent(
                    session = state.session,
                    isJoined = state.currentUserId != null
                        && state.currentUserId in state.session.attendees,
                    modifier = Modifier.padding(innerPadding),
                    onRsvp = { viewModel.onAction(SessionDetailAction.RsvpToggled) },
                    onNavigateToGuru = { onNavigateToGuru(guruId) },
                )
            }
        }
    }
}

@Composable
private fun SessionDetailContent(
    session: Session,
    isJoined: Boolean,
    modifier: Modifier = Modifier,
    onRsvp: () -> Unit = {},
    onNavigateToGuru: () -> Unit = {},
) {
    val locale = Locale.getDefault()
    val dateFormat = remember(locale) { SimpleDateFormat("EEEE, dd MMMM yyyy", locale) }
    val formattedDate = if (session.date > 0) dateFormat.format(Date(session.date)) else ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = session.subject.ifBlank { stringResource(R.string.session_default_subject) },
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(16.dp))

        InfoRow(icon = Icons.Filled.Person, text = session.guruNameEn)
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(
            icon = Icons.Filled.AccessTime,
            text = if (formattedDate.isNotBlank()) {
                "$formattedDate\n${session.startTime} - ${session.endTime}"
            } else {
                "${session.startTime} - ${session.endTime}"
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(
            icon = Icons.Filled.LocationOn,
            text = session.venue.ifBlank { stringResource(R.string.session_venue_tbd) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(
            icon = Icons.Filled.EventSeat,
            text = stringResource(R.string.seats_info, session.attendeeCount, session.maxStudents),
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (session.description.isNotBlank()) {
            Text(stringResource(R.string.about_section), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = session.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        when {
            isJoined -> {
                OutlinedButton(
                    onClick = onRsvp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.leave_button), style = MaterialTheme.typography.labelLarge)
                }
            }
            session.isFull -> {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = false,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.full_label), style = MaterialTheme.typography.labelLarge)
                }
            }
            else -> {
                Button(
                    onClick = onRsvp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.attend_button), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateToGuru,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                stringResource(R.string.view_guru_profile),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_SessionDetail() {
    NimmaGuruTheme {
        SessionDetailContent(
            session = Session(
                id = "1",
                subject = "Mathematics Basics",
                guruNameEn = "Ramesh Kumar",
                date = System.currentTimeMillis() + 86400000,
                startTime = "10:00 AM",
                endTime = "12:00 PM",
                venue = "Samudaya Bhavana, Hunsur",
                description = "We will cover basic algebra and geometry.",
                maxStudents = 20,
            ),
            isJoined = false,
        )
    }
}
