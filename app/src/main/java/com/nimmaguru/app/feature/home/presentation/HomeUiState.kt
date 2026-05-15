package com.nimmaguru.app.feature.home.presentation

import androidx.annotation.StringRes
import com.nimmaguru.app.core.model.AppreciationNote
import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.core.model.Session

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val topGurus: List<Guru> = emptyList(),
        val upcomingSessions: List<Session> = emptyList(),
        val recentAppreciations: List<AppreciationNote> = emptyList(),
        val joinedSessions: List<Session> = emptyList(),
        val showOnboardingPrompt: Boolean = false,
    ) : HomeUiState
    data class Error(@StringRes val messageRes: Int) : HomeUiState
}

sealed interface HomeAction {
    data object Retry : HomeAction
}
