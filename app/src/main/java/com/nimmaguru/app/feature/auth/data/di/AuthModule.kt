package com.nimmaguru.app.feature.auth.data.di

import com.nimmaguru.app.feature.auth.data.repository.AuthRepositoryImpl
import com.nimmaguru.app.feature.auth.data.repository.UserRepositoryImpl
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding AuthRepository interface to its implementation.
 * R-DI-01: Interfaces use @Binds.
 * R-DI-03: One @Module per data source.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
