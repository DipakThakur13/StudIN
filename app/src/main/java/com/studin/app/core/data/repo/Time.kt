package com.studin.app.core.data.repo

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Injectable clock for deterministic tests. */
interface AppClock {
    fun now(): Long
    fun zone(): ZoneId = ZoneId.systemDefault()
    fun today(): Long = LocalDate.now(zone()).toEpochDay()
    fun dayOf(wallMillis: Long): Long = Instant.ofEpochMilli(wallMillis).atZone(zone()).toLocalDate().toEpochDay()
    fun startOfDay(epochDay: Long): Long = LocalDate.ofEpochDay(epochDay).atStartOfDay(zone()).toInstant().toEpochMilli()
}

object SystemAppClock : AppClock { override fun now(): Long = System.currentTimeMillis() }
