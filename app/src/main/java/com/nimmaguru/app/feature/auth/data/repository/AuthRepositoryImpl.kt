package com.nimmaguru.app.feature.auth.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.nimmaguru.app.BuildConfig
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.PhoneAuthEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    private val _phoneAuthEvents = MutableSharedFlow<PhoneAuthEvent>(
        replay = 1,
        extraBufferCapacity = 4,
    )

    @Volatile
    private var _currentVerificationId: String? = null

    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override val currentUserPhone: String?
        get() = firebaseAuth.currentUser?.phoneNumber

    override val currentVerificationId: String?
        get() = _currentVerificationId

    override val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser?.let { !it.isAnonymous } ?: false

    override fun observeAuthState(): Flow<Boolean> = callbackFlow {
        fun loggedIn(u: com.google.firebase.auth.FirebaseUser?): Boolean =
            u != null && !u.isAnonymous

        trySend(loggedIn(firebaseAuth.currentUser))
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(loggedIn(auth.currentUser))
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun phoneAuthEvents(): Flow<PhoneAuthEvent> = _phoneAuthEvents.asSharedFlow()

    override fun startPhoneVerification(phoneNumber: String, activity: Activity) {
        _currentVerificationId = null
        _phoneAuthEvents.resetReplayCache()

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                authScope.launch {
                    try {
                        val authResult = firebaseAuth.signInWithCredential(credential).await()
                        val uid = authResult.user?.uid
                        if (uid != null) {
                            _phoneAuthEvents.emit(PhoneAuthEvent.AutoVerified(uid))
                        } else {
                            _phoneAuthEvents.emit(PhoneAuthEvent.Failed("Auto-verify produced no user"))
                        }
                    } catch (e: Exception) {
                        _phoneAuthEvents.emit(PhoneAuthEvent.Failed(e.message ?: "Auto-verify failed"))
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val userFacing = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid phone number format."
                    else -> e.message ?: "Verification failed"
                }
                authScope.launch {
                    _phoneAuthEvents.emit(PhoneAuthEvent.Failed(userFacing))
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _currentVerificationId = verificationId
                authScope.launch {
                    _phoneAuthEvents.emit(PhoneAuthEvent.CodeSent(verificationId))
                }
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(Constants.OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override suspend fun verifyOtp(verificationId: String, otp: String): Result<String> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid
            if (uid != null) {
                _currentVerificationId = null
                Result.success(uid)
            } else {
                Result.failure(Exception("Authentication succeeded but user ID is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid
            if (uid != null) {
                Result.success(uid)
            } else {
                Result.failure(Exception("Google Sign-In produced no user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        _currentVerificationId = null
        firebaseAuth.signOut()
    }

    private companion object {
        const val TAG = "NimmaGuruAuth"
    }
}
