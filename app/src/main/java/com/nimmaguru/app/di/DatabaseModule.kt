package com.nimmaguru.app.di

import android.content.Context
import androidx.room.Room
import com.nimmaguru.app.core.database.AppDatabase
import com.nimmaguru.app.core.database.GuruDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nimma_guru_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideGuruDao(database: AppDatabase): GuruDao {
        return database.guruDao()
    }
}
