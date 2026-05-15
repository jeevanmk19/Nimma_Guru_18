package com.nimmaguru.app.feature.profile.presentation

import androidx.annotation.StringRes
import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.core.model.Session

/**
 * UI state for Guru Profile screen.
 * R-KT-06: Sealed hierarchy.
 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val guru: Guru,
        val sessions: List<Session> = emptyList(),
        val currentUserId: String? = null,
    ) : ProfileUiState
    data class Error(@StringRes val messageRes: Int) : ProfileUiState
}

sealed interface ProfileEvent {
    data class ShareProfile(val guru: Guru) : ProfileEvent
    data class NavigateToSession(val sessionId: String, val guruId: String) : ProfileEvent
    data class NavigateToAppreciation(val guruId: String) : ProfileEvent
    data class ShowSnackbar(@StringRes val messageRes: Int) : ProfileEvent
}

sealed interface ProfileAction {
    data object Share : ProfileAction
    data object PostAppreciation : ProfileAction
    data class AttendSession(val sessionId: String) : ProfileAction
    data object Retry : ProfileAction
}
