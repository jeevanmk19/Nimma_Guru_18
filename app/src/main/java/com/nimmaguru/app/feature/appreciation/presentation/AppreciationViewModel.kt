package com.nimmaguru.app.feature.appreciation.presentation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.core.common.ValidationResult
import com.nimmaguru.app.core.common.Validators
import com.nimmaguru.app.core.model.AppreciationNote
import com.nimmaguru.app.feature.appreciation.domain.repository.AppreciationRepository
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppreciationViewModel @Inject constructor(
    private val appreciationRepository: AppreciationRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private companion object {
        const val TAG = "NimmaGuruAppreciationVM"
    }

    private val guruId: String = savedStateHandle.get<String>("guruId") ?: ""

    private val _uiState = MutableStateFlow<AppreciationUiState>(AppreciationUiState.Form())
    val uiState: StateFlow<AppreciationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AppreciationEvent>()
    val events: SharedFlow<AppreciationEvent> = _events.asSharedFlow()

    fun onAction(action: AppreciationAction) {
        val currentState = _uiState.value
        if (currentState !is AppreciationUiState.Form) return

        when (action) {
            is AppreciationAction.MessageChanged -> _uiState.update {
                if (it is AppreciationUiState.Form) it.copy(message = action.message, errorRes = null) else it
            }
            is AppreciationAction.RatingChanged -> _uiState.update {
                if (it is AppreciationUiState.Form) it.copy(rating = action.rating, errorRes = null) else it
            }
            is AppreciationAction.Submit -> submitAppreciation(currentState)
        }
    }

    private fun submitAppreciation(form: AppreciationUiState.Form) {
        Log.d(TAG, "submitAppreciation called")

        if (form.rating !in 1..5) {
            Log.d(TAG, "Rating invalid: ${form.rating}")
            _uiState.update {
                if (it is AppreciationUiState.Form) it.copy(errorRes = R.string.rating_label) else it
            }
            return
        }
        when (val v = Validators.validateAppreciation(form.message)) {
            is ValidationResult.Invalid -> {
                Log.d(TAG, "Message invalid: ${v.errorRes}")
                _uiState.update {
                    if (it is AppreciationUiState.Form) it.copy(errorRes = v.errorRes) else it
                }
                return
            }
            ValidationResult.Valid -> Unit
        }

        val uid = authRepository.currentUserId
        if (uid == null) {
            Log.d(TAG, "User not logged in")
            _uiState.update {
                if (it is AppreciationUiState.Form) it.copy(errorRes = R.string.error_login_required) else it
            }
            return
        }

        Log.d(TAG, "Starting submission for guruId: $guruId, uid: $uid")

        _uiState.update {
            if (it is AppreciationUiState.Form) it.copy(isSubmitting = true) else it
        }

        viewModelScope.launch {
            try {
                // Use Firestore-backed display name (set during BasicOnboarding)
                // and fall back to "Student" only if no profile exists yet.
                val displayName = userRepository.getUser(uid).getOrNull()?.name?.takeIf { it.isNotBlank() }
                    ?: "Student"

                Log.d(TAG, "Display name: $displayName")

                val note = AppreciationNote(
                    guruId = guruId,
                    studentName = displayName,
                    message = form.message.trim(),
                    rating = form.rating,
                )

                Log.d(TAG, "Calling postAppreciation...")
                appreciationRepository.postAppreciation(note)
                    .onSuccess { noteId ->
                        Log.d(TAG, "postAppreciation succeeded! noteId: $noteId")
                        _events.emit(AppreciationEvent.ShowSnackbar(R.string.appreciation_success))
                        _events.emit(AppreciationEvent.NavigateBack)
                    }
                    .onFailure { e ->
                        Log.e(TAG, "postAppreciation failed", e)
                        _uiState.update {
                            if (it is AppreciationUiState.Form) it.copy(
                                isSubmitting = false,
                                errorRes = R.string.error_unknown,
                            ) else it
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in submitAppreciation", e)
                _uiState.update {
                    if (it is AppreciationUiState.Form) it.copy(
                        isSubmitting = false,
                        errorRes = R.string.error_unknown,
                    ) else it
                }
            }
        }
    }
}
