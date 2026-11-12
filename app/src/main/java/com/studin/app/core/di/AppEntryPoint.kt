package com.studin.app.core.di

import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.core.data.repo.ProfileRepository
import com.studin.app.core.data.repo.StudyRepository
import com.studin.app.platform.focus.FocusController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Access to singletons from non-injected code (receivers, tests). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun prefs(): UserPreferences
    fun profiles(): ProfileRepository
    fun study(): StudyRepository
    fun focus(): FocusRepository
    fun focusController(): FocusController
}
