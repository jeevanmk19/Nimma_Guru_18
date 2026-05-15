package com.nimmaguru.app.feature.auth.presentation

import android.app.Activity
import androidx.annotation.StringRes

/**
 * UI state for the Auth flow.
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object OtpSent : AuthUiState
    data object Authenticated : AuthUiState
    data class Error(@StringRes val errorRes: Int) : AuthUiState
}

/**
 * One-time events from Auth flow.
 */
sealed interface AuthEvent {
    data object NavigateToHome : AuthEvent
    data object NavigateToBasicOnboarding : AuthEvent
    data object NavigateToOtp : AuthEvent
    data class ShowSnackbar(@StringRes val messageRes: Int) : AuthEvent
}

/**
 * User actions on the Auth screen.
 */
sealed interface AuthAction {
    data class PhoneNumberChanged(val phone: String) : AuthAction
    data class OtpChanged(val otp: String) : AuthAction
    data class SendOtp(val activity: Activity) : AuthAction
    data object VerifyOtp : AuthAction
    data class ResendOtp(val activity: Activity) : AuthAction
    data object ContinueAsGuest : AuthAction
    data class SignInWithGoogle(val idToken: String) : AuthAction
}
