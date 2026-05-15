package com.nimmaguru.app.feature.profile.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.UserRepository
import com.nimmaguru.app.feature.profile.domain.usecase.ObserveGuruUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val observeGuruUseCase: ObserveGuruUseCase,
) : ViewModel() {

    val uiState: StateFlow<MyProfileUiState> = authRepository.observeAuthState()
        .flatMapLatest { isLoggedIn ->
            val uid = authRepository.currentUserId
            if (isLoggedIn && uid != null) {
                combine(
                    userRepository.observeUser(uid),
                    observeGuruUseCase(uid),
                ) { user, guru ->
                    when {
                        guru != null -> MyProfileUiState.Authenticated(guru)
                        user != null -> MyProfileUiState.Student(user)
                        else -> MyProfileUiState.NoProfile(uid)
                    }
                }
            } else {
                flowOf(MyProfileUiState.NotLoggedIn)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MyProfileUiState.Loading,
        )

    private val _events = MutableSharedFlow<MyProfileEvent>()
    val events: SharedFlow<MyProfileEvent> = _events.asSharedFlow()

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _events.emit(MyProfileEvent.SignedOut)
            _events.emit(MyProfileEvent.ShowSnackbar(R.string.signed_out))
        }
    }
}

sealed interface MyProfileUiState {
    data object Loading : MyProfileUiState
    data object NotLoggedIn : MyProfileUiState
    data class NoProfile(val userId: String) : MyProfileUiState
    data class Student(val user: com.nimmaguru.app.core.model.User) : MyProfileUiState
    data class Authenticated(val guru: com.nimmaguru.app.core.model.Guru) : MyProfileUiState
}

sealed interface MyProfileEvent {
    data object SignedOut : MyProfileEvent
    data class ShowSnackbar(@StringRes val messageRes: Int) : MyProfileEvent
}
