package com.nimmaguru.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.feature.appreciation.domain.repository.AppreciationRepository
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.UserRepository
import com.nimmaguru.app.feature.calendar.domain.repository.SessionRepository
import com.nimmaguru.app.feature.profile.domain.repository.GuruRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel.
 *
 * Wall-of-Fame is now backed by [GuruRepository.observeTopGurus] — fully
 * real-time and ranked server-side by `fameScore` (H11/H12/H26).
 *
 * `isGuru` and the onboarding banner are reactive: as soon as the
 * user's [users.role] flips to "guru", the banner disappears (H26).
 *
 * Retry cancels the previous combine `Job` so we don't leak duplicate
 * collectors that race on `_uiState` (H28).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val guruRepository: GuruRepository,
    private val sessionRepository: SessionRepository,
    private val appreciationRepository: AppreciationRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        loadJob?.cancel()
        _uiState.value = HomeUiState.Loading

        loadJob = viewModelScope.launch {
            authRepository.observeAuthState()
                .flatMapLatest { isLoggedIn ->
                    val userId = authRepository.currentUserId
                    val joinedFlow = if (isLoggedIn && userId != null) {
                        sessionRepository.observeJoinedSessions(userId)
                    } else {
                        flowOf(emptyList())
                    }
                    val isGuruFlow = if (isLoggedIn && userId != null) {
                        userRepository.observeUser(userId)
                    } else {
                        flowOf(null)
                    }

                    combine(
                        guruRepository.observeTopGurus(limit = 10),
                        sessionRepository.observeUpcomingSessions(),
                        appreciationRepository.observeRecentAppreciations(limit = 5),
                        joinedFlow,
                        isGuruFlow,
                    ) { gurus, sessions, appreciations, joined, user ->
                        HomeUiState.Success(
                            topGurus = gurus,
                            upcomingSessions = sessions,
                            recentAppreciations = appreciations,
                            joinedSessions = joined,
                            showOnboardingPrompt = isLoggedIn
                                && user != null
                                && user.role != "guru",
                        ) as HomeUiState
                    }
                }
                .catch { _uiState.value = HomeUiState.Error(R.string.error_load_home) }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.Retry -> loadHomeData()
        }
    }
}
