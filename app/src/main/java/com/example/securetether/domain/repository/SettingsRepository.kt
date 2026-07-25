package com.example.securetether.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val biometricEnabled: Flow<Boolean>
    val autoLockTimer: Flow<Long> // in milliseconds, 0 for never
    val stealthModeEnabled: Flow<Boolean>
    val themeMode: Flow<Int> // 0: System, 1: Light, 2: Dark
    val exportPath: Flow<String>

    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setAutoLockTimer(millis: Long)
    suspend fun setStealthModeEnabled(enabled: Boolean)
    suspend fun setThemeMode(mode: Int)
    suspend fun setExportPath(path: String)
}
