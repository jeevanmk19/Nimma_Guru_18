package com.nimmaguru.app.feature.auth.domain.usecase

import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe authentication state.
 * R-ARCH-04: UseCase MUST NOT depend on Android framework classes.
 */
class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<Boolean> = authRepository.observeAuthState()
}
