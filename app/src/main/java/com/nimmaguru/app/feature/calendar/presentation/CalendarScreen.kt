package com.nimmaguru.app.feature.calendar.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DayChipOption(val key: DayKey, val labelRes: Int)

private val DAY_CHIP_OPTIONS = listOf(
    DayChipOption(DayKey.All, R.string.day_filter_all),
    DayChipOption(DayKey.Mon, R.string.day_mon),
    DayChipOption(DayKey.Tue, R.string.day_tue),
    DayChipOption(DayKey.Wed, R.string.day_wed),
    DayChipOption(DayKey.Thu, R.string.day_thu),
    DayChipOption(DayKey.Fri, R.string.day_fri),
    DayChipOption(DayKey.Sat, R.string.day_sat),
    DayChipOption(DayKey.Sun, R.string.day_sun),
)

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
    onNavigateToSession: (String, String) -> Unit = { _, _ -> },
    onNavigateToCreateSession: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CalendarEvent.NavigateToSession ->
                    onNavigateToSession(event.sessionId, event.guruId)
                is CalendarEvent.NavigateToCreateSession ->
                    onNavigateToCreateSession()
                is CalendarEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val state = uiState
            if (state is CalendarUiState.Success && state.isGuru) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onAction(CalendarAction.CreateSessionClicked) },
                    icon = {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.cd_create_session_fab),
                        )
                    },
                    text = { Text(stringResource(R.string.session_create_button)) },
                )
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is CalendarUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is CalendarUiState.Success -> {
                CalendarContent(
                    modifier = Modifier.padding(innerPadding),
                    sessions = state.sessions,
                    selectedDay = state.selectedDay,
                    currentUserId = state.currentUserId,
                    onAction = viewModel::onAction,
                )
            }
            is CalendarUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(state.messageRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.onAction(CalendarAction.Retry) }) {
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
fun CalendarContent(
    modifier: Modifier = Modifier,
    sessions: List<Session> = emptyList(),
    selectedDay: DayKey = DayKey.All,
    currentUserId: String? = null,
    onAction: (CalendarAction) -> Unit = {},
) {
    val filteredSessions = if (selectedDay == DayKey.All) {
        sessions
    } else {
        val cal = Calendar.getInstance()
        sessions.filter { session ->
            cal.timeInMillis = session.date
            cal.get(Calendar.DAY_OF_WEEK) == selectedDay.calendarValue
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.calendar_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DAY_CHIP_OPTIONS.forEach { opt ->
                FilterChip(
                    selected = selectedDay == opt.key,
                    onClick = { onAction(CalendarAction.DayFilterChanged(opt.key)) },
                    label = {
                        Text(
                            stringResource(opt.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    modifier = Modifier.height(48.dp), // R-COMP-03
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_sessions_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        currentUserId = currentUserId,
                        onSessionClick = {
                            onAction(CalendarAction.SessionClicked(session.id, session.guruId))
                        },
                        onRsvpClick = { onAction(CalendarAction.RsvpToggled(session.id)) },
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: Session,
    currentUserId: String?,
    onSessionClick: () -> Unit,
    onRsvpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val dateFormat = remember(locale) { SimpleDateFormat("EEE, dd MMM", locale) }
    val formattedDate = if (session.date > 0) dateFormat.format(Date(session.date)) else ""
    val isJoined = currentUserId != null && currentUserId in session.attendees

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onSessionClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.subject.ifBlank { session.description },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    session.guruNameEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (formattedDate.isNotBlank()) "$formattedDate · ${session.startTime} - ${session.endTime}"
                    else "${session.startTime} - ${session.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.EventSeat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.seats_info, session.attendeeCount, session.maxStudents),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (session.isFull) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                when {
                    isJoined -> {
                        OutlinedButton(
                            onClick = onRsvpClick,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(
                                stringResource(R.string.joined_status),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    session.isFull -> {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(stringResource(R.string.full_label))
                        }
                    }
                    else -> {
                        Button(
                            onClick = onRsvpClick,
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                stringResource(R.string.attend_button),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_CalendarScreen() {
    NimmaGuruTheme {
        CalendarContent(
            sessions = listOf(
                Session(
                    id = "1", subject = "Math Basics", guruNameEn = "Ramesh Kumar",
                    date = System.currentTimeMillis() + 86400000,
                    startTime = "10:00 AM", endTime = "12:00 PM",
                    maxStudents = 20, attendees = listOf("u1", "u2"),
                ),
            ),
        )
    }
}
