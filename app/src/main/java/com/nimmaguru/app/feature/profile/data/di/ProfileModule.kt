package com.nimmaguru.app.feature.profile.data.di

import com.nimmaguru.app.feature.profile.data.repository.GuruRepositoryImpl
import com.nimmaguru.app.feature.profile.domain.repository.GuruRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Guru profile bindings.
 * R-DI-01: @Binds for interface binding.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindGuruRepository(impl: GuruRepositoryImpl): GuruRepository
}
