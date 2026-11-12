package com.studin.app.core.di

import android.content.Context
import androidx.room.Room
import com.studin.app.core.data.db.StudinDatabase
import com.studin.app.core.data.repo.AppClock
import com.studin.app.core.data.repo.SystemAppClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context): StudinDatabase =
        Room.databaseBuilder(ctx, StudinDatabase::class.java, StudinDatabase.NAME)
            .addMigrations(*StudinDatabase.MIGRATIONS)
            .build()

    @Provides @Singleton fun clock(): AppClock = SystemAppClock

    @Provides @Singleton @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
