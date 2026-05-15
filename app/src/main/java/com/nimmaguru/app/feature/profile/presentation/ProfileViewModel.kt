package com.nimmaguru.app.feature.profile.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.calendar.domain.usecase.CancelRsvpUseCase
import com.nimmaguru.app.feature.calendar.domain.usecase.ObserveSessionsByGuruUseCase
import com.nimmaguru.app.feature.calendar.domain.usecase.RsvpToSessionUseCase
import com.nimmaguru.app.feature.profile.domain.usecase.ObserveGuruUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeGuruUseCase: ObserveGuruUseCase,
    private val observeSessionsUseCase: ObserveSessionsByGuruUseCase,
    private val rsvpUseCase: RsvpToSessionUseCase,
    private val cancelRsvpUseCase: CancelRsvpUseCase,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val guruId: String = savedStateHandle.get<String>("guruId") ?: ""

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        if (guruId.isNotBlank()) loadGuru()
    }

    private fun loadGuru() {
        viewModelScope.launch {
            combine(
                observeGuruUseCase(guruId),
                observeSessionsUseCase(guruId),
            ) { guru, sessions ->
                if (guru != null) {
                    ProfileUiState.Success(
                        guru = guru,
                        sessions = sessions,
                        currentUserId = authRepository.currentUserId,
                    )
                } else {
                    ProfileUiState.Error(R.string.error_guru_not_found)
                }
            }
                .catch { _uiState.value = ProfileUiState.Error(R.string.error_load_profile) }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.Share -> {
                val current = _uiState.value as? ProfileUiState.Success ?: return
                viewModelScope.launch { _events.emit(ProfileEvent.ShareProfile(current.guru)) }
            }
            is ProfileAction.PostAppreciation -> {
                viewModelScope.launch { _events.emit(ProfileEvent.NavigateToAppreciation(guruId)) }
            }
            is ProfileAction.AttendSession -> attendSession(action.sessionId)
            is ProfileAction.Retry -> loadGuru()
        }
    }

    private fun attendSession(sessionId: String) {
        val state = _uiState.value as? ProfileUiState.Success ?: return
        val userId = authRepository.currentUserId ?: run {
            viewModelScope.launch { _events.emit(ProfileEvent.ShowSnackbar(R.string.error_login_required)) }
            return
        }
        val session = state.sessions.firstOrNull { it.id == sessionId } ?: return
        val isJoined = userId in session.attendees

        viewModelScope.launch {
            val result = if (isJoined) cancelRsvpUseCase(sessionId, userId)
                         else rsvpUseCase(sessionId, userId)
            result
                .onSuccess {
                    _events.emit(
                        ProfileEvent.ShowSnackbar(
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
                    _events.emit(ProfileEvent.ShowSnackbar(msg))
                }
        }
    }
}
