package com.nimmaguru.app.feature.auth.domain.repository

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/**
 * Auth repository interface — defined in domain layer.
 */
interface AuthRepository {
    val currentUserId: String?
    val currentUserPhone: String?
    val isLoggedIn: Boolean

    /**
     * Latest verificationId received from a `PhoneAuthEvent.CodeSent` emission.
     */
    val currentVerificationId: String?

    fun observeAuthState(): Flow<Boolean>

    /**
     * Stream of phone-auth lifecycle events.
     */
    fun phoneAuthEvents(): Flow<PhoneAuthEvent>

    /**
     * Kicks off the phone-auth flow.
     */
    fun startPhoneVerification(phoneNumber: String, activity: Activity)

    /**
     * Submits the user-typed OTP.
     */
    suspend fun verifyOtp(verificationId: String, otp: String): Result<String>

    /**
     * Signs in with Google using a credential (ID token).
     */
    suspend fun signInWithGoogle(idToken: String): Result<String>

    suspend fun signOut()
}

/** Domain-level phone auth events. */
sealed interface PhoneAuthEvent {
    data class CodeSent(val verificationId: String) : PhoneAuthEvent
    data class AutoVerified(val uid: String) : PhoneAuthEvent
    data class Failed(val reason: String) : PhoneAuthEvent
}
