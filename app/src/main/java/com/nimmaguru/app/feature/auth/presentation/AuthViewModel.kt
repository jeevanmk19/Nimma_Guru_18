package com.nimmaguru.app.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.PhoneAuthEvent
import com.nimmaguru.app.feature.auth.domain.repository.UserRepository
import com.nimmaguru.app.feature.auth.domain.usecase.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private var codeSentHandled: Boolean = false

    init {
        viewModelScope.launch {
            authRepository.phoneAuthEvents().collect { event ->
                when (event) {
                    is PhoneAuthEvent.CodeSent -> {
                        _uiState.value = AuthUiState.OtpSent
                        if (!codeSentHandled) {
                            codeSentHandled = true
                            _events.emit(AuthEvent.NavigateToOtp)
                        }
                    }
                    is PhoneAuthEvent.AutoVerified -> {
                        _uiState.value = AuthUiState.Authenticated
                        handlePostLoginNavigation(event.uid)
                    }
                    is PhoneAuthEvent.Failed -> {
                        _uiState.value = AuthUiState.Error(R.string.error_otp_send_failed)
                    }
                }
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.PhoneNumberChanged -> _phoneNumber.value = action.phone
            is AuthAction.OtpChanged -> _otpCode.value = action.otp
            is AuthAction.SendOtp -> {} // Not used anymore if we remove phone
            is AuthAction.VerifyOtp -> verifyOtp()
            is AuthAction.ResendOtp -> {} // Not used anymore
            is AuthAction.ContinueAsGuest -> continueAsGuest()
            is AuthAction.SignInWithGoogle -> signInWithGoogle(action.idToken)
        }
    }

    private fun signInWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken)
                .onSuccess { uid ->
                    _uiState.value = AuthUiState.Authenticated
                    handlePostLoginNavigation(uid)
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(R.string.error_verification_failed)
                }
        }
    }

    private fun verifyOtp() {
        val otp = _otpCode.value
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error(R.string.error_invalid_otp)
            return
        }
        val vId = authRepository.currentVerificationId
        if (vId.isNullOrEmpty()) {
            _uiState.value = AuthUiState.Error(R.string.error_verification_failed)
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            verifyOtpUseCase(vId, otp)
                .onSuccess { uid ->
                    _uiState.value = AuthUiState.Authenticated
                    handlePostLoginNavigation(uid)
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(R.string.error_verification_failed)
                }
        }
    }

    private fun handlePostLoginNavigation(uid: String) {
        viewModelScope.launch {
            userRepository.getUser(uid)
                .onSuccess { user ->
                    if (user != null) {
                        _events.emit(AuthEvent.NavigateToHome)
                    } else {
                        _events.emit(AuthEvent.NavigateToBasicOnboarding)
                    }
                }
                .onFailure {
                    _events.emit(AuthEvent.NavigateToBasicOnboarding)
                }
        }
    }

    private fun continueAsGuest() {
        viewModelScope.launch {
            _events.emit(AuthEvent.NavigateToHome)
        }
    }
}
