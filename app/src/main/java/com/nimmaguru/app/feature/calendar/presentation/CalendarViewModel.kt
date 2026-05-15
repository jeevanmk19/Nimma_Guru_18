package com.nimmaguru.app.feature.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.calendar.domain.usecase.CancelRsvpUseCase
import com.nimmaguru.app.feature.calendar.domain.usecase.ObserveUpcomingSessionsUseCase
import com.nimmaguru.app.feature.calendar.domain.usecase.RsvpToSessionUseCase
import com.nimmaguru.app.feature.profile.domain.repository.GuruRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Calendar VM. RSVP is now a real toggle (join ↔ leave) with localized
 * snackbar feedback (H17/H10).
 *
 * Layering: AuthRepository.currentUserId is used instead of FirebaseAuth
 * directly (R-FB-05).
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val observeUpcomingSessionsUseCase: ObserveUpcomingSessionsUseCase,
    private val rsvpToSessionUseCase: RsvpToSessionUseCase,
    private val cancelRsvpUseCase: CancelRsvpUseCase,
    private val authRepository: AuthRepository,
    private val guruRepository: GuruRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CalendarEvent>()
    val events: SharedFlow<CalendarEvent> = _events.asSharedFlow()

    private var sessionsJob: Job? = null

    init {
        loadSessions()
    }

    private fun loadSessions() {
        sessionsJob?.cancel()
        _uiState.value = CalendarUiState.Loading
        sessionsJob = viewModelScope.launch {
            // Determine if current user is a published Guru (drives the FAB visibility)
            val userId = authRepository.currentUserId
            val isGuru = if (userId != null) guruRepository.getGuru(userId).isSuccess else false

            observeUpcomingSessionsUseCase()
                .catch {
                    _uiState.value = CalendarUiState.Error(R.string.error_load_sessions)
                }
                .collect { sessions ->
                    val current = _uiState.value
                    val selectedDay = (current as? CalendarUiState.Success)?.selectedDay ?: DayKey.All
                    _uiState.value = CalendarUiState.Success(
                        sessions = sessions,
                        selectedDay = selectedDay,
                        currentUserId = userId,
                        isGuru = isGuru,
                    )
                }
        }
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.DayFilterChanged -> {
                _uiState.update { current ->
                    if (current is CalendarUiState.Success) current.copy(selectedDay = action.day)
                    else current
                }
            }
            is CalendarAction.SessionClicked -> {
                viewModelScope.launch {
                    _events.emit(CalendarEvent.NavigateToSession(action.sessionId, action.guruId))
                }
            }
            is CalendarAction.RsvpToggled -> toggleRsvp(action.sessionId)
            is CalendarAction.CreateSessionClicked -> {
                viewModelScope.launch {
                    _events.emit(CalendarEvent.NavigateToCreateSession)
                }
            }
            is CalendarAction.Retry -> loadSessions()
        }
    }

    private fun toggleRsvp(sessionId: String) {
        val state = _uiState.value as? CalendarUiState.Success ?: return
        val userId = state.currentUserId ?: run {
            viewModelScope.launch {
                _events.emit(CalendarEvent.ShowSnackbar(R.string.error_login_required))
            }
            return
        }
        val session = state.sessions.firstOrNull { it.id == sessionId } ?: return
        val isJoined = userId in session.attendees

        viewModelScope.launch {
            val result = if (isJoined) cancelRsvpUseCase(sessionId, userId)
                         else rsvpToSessionUseCase(sessionId, userId)
            result
                .onSuccess {
                    _events.emit(
                        CalendarEvent.ShowSnackbar(
                            if (isJoined) R.string.rsvp_cancelled else R.string.rsvp_success,
                        )
                    )
                }
                .onFailure { e ->
                    val msg = when {
                        e.message?.contains("full", ignoreCase = true) == true ->
                            R.string.error_session_full
                        e.message?.contains("already", ignoreCase = true) == true ->
                            R.string.error_already_joined
                        else -> R.string.error_unknown
                    }
                    _events.emit(CalendarEvent.ShowSnackbar(msg))
                }
        }
    }
}
