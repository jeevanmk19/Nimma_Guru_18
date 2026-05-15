package com.nimmaguru.app.feature.calendar.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.calendar.domain.usecase.CancelRsvpUseCase
import com.nimmaguru.app.feature.calendar.domain.usecase.ObserveSessionUseCase
import com.nimmaguru.app.feature.calendar.domain.usecase.RsvpToSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives [SessionDetailScreen]. Replaces the previous direct
 * Firestore-from-composable I/O (R-FB-05 / R-ARCH-02).
 */
@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val rsvpUseCase: RsvpToSessionUseCase,
    private val cancelRsvpUseCase: CancelRsvpUseCase,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId") ?: ""

    private val _uiState = MutableStateFlow<SessionDetailUiState>(SessionDetailUiState.Loading)
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SessionDetailEvent>()
    val events: SharedFlow<SessionDetailEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun onAction(action: SessionDetailAction) = when (action) {
        is SessionDetailAction.RsvpToggled -> toggleRsvp()
        is SessionDetailAction.Retry -> load()
    }

    private fun load() {
        if (sessionId.isBlank()) {
            _uiState.value = SessionDetailUiState.Error(R.string.error_load_sessions)
            return
        }
        _uiState.value = SessionDetailUiState.Loading
        viewModelScope.launch {
            observeSessionUseCase(sessionId)
                .onSuccess { session ->
                    _uiState.value = SessionDetailUiState.Success(
                        session = session,
                        currentUserId = authRepository.currentUserId,
                    )
                }
                .onFailure {
                    _uiState.value = SessionDetailUiState.Error(R.string.error_load_sessions)
                }
        }
    }

    private fun toggleRsvp() {
        val current = _uiState.value as? SessionDetailUiState.Success ?: return
        val userId = authRepository.currentUserId ?: run {
            viewModelScope.launch {
                _events.emit(SessionDetailEvent.ShowSnackbar(R.string.error_login_required))
            }
            return
        }
        val isJoined = userId in current.session.attendees

        viewModelScope.launch {
            val result = if (isJoined) {
                cancelRsvpUseCase(sessionId, userId)
            } else {
                rsvpUseCase(sessionId, userId)
            }
            result
                .onSuccess {
                    _events.emit(
                        SessionDetailEvent.ShowSnackbar(
                            if (isJoined) R.string.rsvp_cancelled else R.string.rsvp_success,
                        )
                    )
                    load() // Refresh attendee count
                }
                .onFailure { e ->
                    val msg = when {
                        e.message?.contains("full", ignoreCase = true) == true ->
                            R.string.error_session_full
                        e.message?.contains("already", ignoreCase = true) == true ->
                            R.string.error_already_joined
                        else -> R.string.error_unknown
                    }
                    _events.emit(SessionDetailEvent.ShowSnackbar(msg))
                }
        }
    }
}

sealed interface SessionDetailUiState {
    data object Loading : SessionDetailUiState
    data class Success(
        val session: Session,
        val currentUserId: String?,
    ) : SessionDetailUiState
    data class Error(@StringRes val messageRes: Int) : SessionDetailUiState
}

sealed interface SessionDetailEvent {
    data class ShowSnackbar(@StringRes val messageRes: Int) : SessionDetailEvent
}

sealed interface SessionDetailAction {
    data object RsvpToggled : SessionDetailAction
    data object Retry : SessionDetailAction
}
