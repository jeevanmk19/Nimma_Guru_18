package com.nimmaguru.app.feature.auth.domain.usecase

import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case to verify OTP and sign in the user.
 *
 * R-ARCH-04: Single operator fun invoke() per use case.
 * R-FB-05: Domain has no Firebase types — repository handles credential creation.
 */
class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        verificationId: String,
        otp: String,
    ): Result<String> = authRepository.verifyOtp(verificationId, otp)
}
