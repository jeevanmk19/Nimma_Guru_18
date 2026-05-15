package com.nimmaguru.app.feature.appreciation.data.di

import com.nimmaguru.app.feature.appreciation.data.repository.AppreciationRepositoryImpl
import com.nimmaguru.app.feature.appreciation.domain.repository.AppreciationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppreciationModule {

    @Binds
    @Singleton
    abstract fun bindAppreciationRepository(impl: AppreciationRepositoryImpl): AppreciationRepository
}
