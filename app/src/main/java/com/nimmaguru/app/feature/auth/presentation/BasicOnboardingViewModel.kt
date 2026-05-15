package com.nimmaguru.app.feature.auth.presentation

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.core.common.ValidationResult
import com.nimmaguru.app.core.common.Validators
import com.nimmaguru.app.core.model.User
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BasicOnboardingUiState {
    data class Form(
        val name: String = "",
        val district: String = "",
        @StringRes val errorRes: Int? = null,
    ) : BasicOnboardingUiState

    data object Loading : BasicOnboardingUiState
    data object Success : BasicOnboardingUiState
}

sealed interface BasicOnboardingEvent {
    data object NavigateToHome : BasicOnboardingEvent
}

sealed interface BasicOnboardingAction {
    data class NameChanged(val value: String) : BasicOnboardingAction
    data class DistrictChanged(val value: String) : BasicOnboardingAction
    data object Submit : BasicOnboardingAction
}

@HiltViewModel
class BasicOnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BasicOnboardingUiState>(BasicOnboardingUiState.Form())
    val uiState: StateFlow<BasicOnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BasicOnboardingEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<BasicOnboardingEvent> = _events.asSharedFlow()

    fun onAction(action: BasicOnboardingAction) {
        when (action) {
            is BasicOnboardingAction.NameChanged -> _uiState.update {
                if (it is BasicOnboardingUiState.Form) it.copy(name = action.value, errorRes = null) else it
            }
            is BasicOnboardingAction.DistrictChanged -> _uiState.update {
                if (it is BasicOnboardingUiState.Form) it.copy(district = action.value, errorRes = null) else it
            }
            is BasicOnboardingAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _uiState.value as? BasicOnboardingUiState.Form ?: return
        val nameRes = Validators.validateName(current.name)
        val villageRes = Validators.validateVillage(current.district)

        when {
            nameRes is ValidationResult.Invalid ->
                _uiState.value = current.copy(errorRes = nameRes.errorRes)
            villageRes is ValidationResult.Invalid ->
                _uiState.value = current.copy(errorRes = villageRes.errorRes)
            else -> persist(current.name.trim(), current.district.trim())
        }
    }

    private fun persist(name: String, district: String) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = (_uiState.value as? BasicOnboardingUiState.Form)
                ?.copy(errorRes = R.string.error_user_not_logged_in)
                ?: BasicOnboardingUiState.Form(name, district, R.string.error_user_not_logged_in)
            return
        }
        val phone = authRepository.currentUserPhone.orEmpty()

        _uiState.value = BasicOnboardingUiState.Loading
        viewModelScope.launch {
            val user = User(
                id = uid,
                name = name,
                district = district,
                phone = phone,
                role = Constants.ROLE_STUDENT,
                langPref = "kn",
            )
            userRepository.saveUser(user)
                .onSuccess {
                    _uiState.value = BasicOnboardingUiState.Success
                    _events.emit(BasicOnboardingEvent.NavigateToHome)
                }
                .onFailure { exception ->
                    Log.e("BasicOnboardingVM", "Failed to save user profile", exception)
                    _uiState.value = BasicOnboardingUiState.Form(
                        name = name,
                        district = district,
                        errorRes = R.string.error_unknown,
                    )
                }
        }
    }
}
