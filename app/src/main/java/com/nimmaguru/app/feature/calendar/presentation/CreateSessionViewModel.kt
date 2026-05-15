package com.nimmaguru.app.feature.calendar.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.core.common.ValidationResult
import com.nimmaguru.app.core.common.Validators
import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.calendar.domain.repository.SessionRepository
import com.nimmaguru.app.feature.profile.domain.repository.GuruRepository
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

data class CreateSessionFormState(
    val subject: String = "",
    val description: String = "",
    val venue: String = "",
    val dateText: String = "", // yyyy-mm-dd
    val startTime: String = "",
    val endTime: String = "",
    val maxStudents: String = "15",
    val isSubmitting: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

sealed interface CreateSessionUiState {
    data class Form(val form: CreateSessionFormState) : CreateSessionUiState
    data object Submitted : CreateSessionUiState
}

sealed interface CreateSessionEvent {
    data object NavigateBack : CreateSessionEvent
    data class ShowSnackbar(@StringRes val messageRes: Int) : CreateSessionEvent
}

sealed interface CreateSessionAction {
    data class SubjectChanged(val v: String) : CreateSessionAction
    data class DescriptionChanged(val v: String) : CreateSessionAction
    data class VenueChanged(val v: String) : CreateSessionAction
    data class DateChanged(val v: String) : CreateSessionAction
    data class StartTimeChanged(val v: String) : CreateSessionAction
    data class EndTimeChanged(val v: String) : CreateSessionAction
    data class MaxStudentsChanged(val v: String) : CreateSessionAction
    data object Submit : CreateSessionAction
}

@HiltViewModel
class CreateSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val guruRepository: GuruRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateSessionUiState>(
        CreateSessionUiState.Form(CreateSessionFormState())
    )
    val uiState: StateFlow<CreateSessionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateSessionEvent>()
    val events: SharedFlow<CreateSessionEvent> = _events.asSharedFlow()

    fun onAction(action: CreateSessionAction) {
        when (action) {
            is CreateSessionAction.SubjectChanged -> updateForm { it.copy(subject = action.v, errorRes = null) }
            is CreateSessionAction.DescriptionChanged -> updateForm { it.copy(description = action.v, errorRes = null) }
            is CreateSessionAction.VenueChanged -> updateForm { it.copy(venue = action.v, errorRes = null) }
            is CreateSessionAction.DateChanged -> updateForm { it.copy(dateText = action.v, errorRes = null) }
            is CreateSessionAction.StartTimeChanged -> updateForm { it.copy(startTime = action.v, errorRes = null) }
            is CreateSessionAction.EndTimeChanged -> updateForm { it.copy(endTime = action.v, errorRes = null) }
            is CreateSessionAction.MaxStudentsChanged -> updateForm {
                if (action.v.all { c -> c.isDigit() }) it.copy(maxStudents = action.v, errorRes = null) else it
            }
            is CreateSessionAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = (_uiState.value as? CreateSessionUiState.Form)?.form ?: return

        if (current.subject.isBlank() || current.subject.length > Constants.SESSION_SUBJECT_MAX_LENGTH) {
            updateForm { it.copy(errorRes = R.string.error_session_subject_required) }; return
        }
        if (current.venue.isBlank()) {
            updateForm { it.copy(errorRes = R.string.error_session_venue_required) }; return
        }
        when (val v = Validators.validateFutureDate(current.dateText)) {
            is ValidationResult.Invalid -> {
                updateForm { it.copy(errorRes = v.errorRes) }; return
            }
            ValidationResult.Valid -> Unit
        }
        if (current.startTime.isBlank() || current.endTime.isBlank()) {
            updateForm { it.copy(errorRes = R.string.error_session_subject_required) }; return
        }
        val maxStudents = current.maxStudents.toIntOrNull()?.coerceAtLeast(1) ?: 15

        updateForm { it.copy(isSubmitting = true, errorRes = null) }

        viewModelScope.launch {
            val uid = authRepository.currentUserId
            if (uid == null) {
                updateForm { it.copy(isSubmitting = false, errorRes = R.string.error_login_required) }
                return@launch
            }

            // Verify Guru profile exists (defense in depth — rules also enforce)
            val guruResult = guruRepository.getGuru(uid)
            if (guruResult.isFailure) {
                updateForm { it.copy(isSubmitting = false, errorRes = R.string.error_only_gurus_create) }
                _events.emit(CreateSessionEvent.ShowSnackbar(R.string.error_only_gurus_create))
                return@launch
            }
            val guru = guruResult.getOrNull()!!

            val dateMs = parseYyyyMmDd(current.dateText) ?: run {
                updateForm { it.copy(isSubmitting = false, errorRes = R.string.error_session_date_required) }
                return@launch
            }

            val session = Session(
                guruId = uid,
                guruNameEn = guru.nameEn,
                guruNameKn = guru.nameKn,
                guruPhotoUrl = guru.photoUrl,
                subject = current.subject.trim(),
                description = current.description.trim(),
                date = dateMs,
                startTime = current.startTime.trim(),
                endTime = current.endTime.trim(),
                venue = current.venue.trim(),
                maxStudents = maxStudents,
                status = Constants.SESSION_UPCOMING,
            )

            sessionRepository.createSession(session)
                .onSuccess {
                    _uiState.value = CreateSessionUiState.Submitted
                    _events.emit(CreateSessionEvent.ShowSnackbar(R.string.session_created_success))
                    _events.emit(CreateSessionEvent.NavigateBack)
                }
                .onFailure {
                    updateForm { it.copy(isSubmitting = false, errorRes = R.string.error_unknown) }
                }
        }
    }

    private fun parseYyyyMmDd(text: String): Long? {
        val parts = text.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return java.util.Calendar.getInstance().apply {
            set(y, m - 1, d, 9, 0, 0) // default to 9am for date sorting
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun updateForm(transform: (CreateSessionFormState) -> CreateSessionFormState) {
        _uiState.update { current ->
            if (current is CreateSessionUiState.Form) {
                CreateSessionUiState.Form(transform(current.form))
            } else current
        }
    }
}
