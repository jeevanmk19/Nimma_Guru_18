package com.nimmaguru.app.feature.calendar.data.di

import com.nimmaguru.app.feature.calendar.data.repository.SessionRepositoryImpl
import com.nimmaguru.app.feature.calendar.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
