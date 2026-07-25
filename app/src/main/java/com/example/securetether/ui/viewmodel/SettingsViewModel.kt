package com.example.securetether.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securetether.domain.repository.SettingsRepository
import com.example.securetether.domain.security.KeystoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val autoLockTimer: Long = 0L,
    val stealthModeEnabled: Boolean = false,
    val themeMode: Int = 0,
    val exportPath: String = "",
    val version: String = "1.0.0"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val keystoreManager: KeystoreManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.biometricEnabled,
        settingsRepository.autoLockTimer,
        settingsRepository.stealthModeEnabled,
        settingsRepository.themeMode,
        settingsRepository.exportPath
    ) { biometric, autoLock, stealth, theme, export ->
        SettingsUiState(
            biometricEnabled = biometric,
            autoLockTimer = autoLock,
            stealthModeEnabled = stealth,
            themeMode = theme,
            exportPath = export
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

    fun setAutoLockTimer(millis: Long) {
        viewModelScope.launch {
            settingsRepository.setAutoLockTimer(millis)
        }
    }

    fun setStealthModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStealthModeEnabled(enabled)
        }
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun validatePin(pin: String): Boolean {
        return keystoreManager.validatePin(pin)
    }

    fun updatePin(newPin: String) {
        viewModelScope.launch {
            keystoreManager.savePin(newPin)
        }
    }
}
