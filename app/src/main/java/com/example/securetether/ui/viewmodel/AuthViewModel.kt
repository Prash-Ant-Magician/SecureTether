package com.example.securetether.ui.viewmodel

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AuthUiState(
    val pin: String = "",
    val error: String? = null,
    val isAuthenticating: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isPinSet: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val keystoreManager: com.example.securetether.domain.security.KeystoreManager,
    private val settingsRepository: com.example.securetether.domain.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val biometricEnabled = settingsRepository.biometricEnabled

    init {
        _uiState.update { it.copy(isPinSet = keystoreManager.isPinSet()) }
    }

    fun onPinChanged(newPin: String) {
        if (newPin.length <= 4) {
            _uiState.update { it.copy(pin = newPin, error = null) }
            if (newPin.length == 4) {
                if (_uiState.value.isPinSet) {
                    validatePin(newPin)
                } else {
                    setupPin(newPin)
                }
            }
        }
    }

    private fun validatePin(pin: String) {
        if (keystoreManager.validatePin(pin)) {
            _uiState.update { it.copy(isAuthenticated = true) }
        } else {
            _uiState.update { it.copy(pin = "", error = "Invalid PIN") }
        }
    }

    private fun setupPin(pin: String) {
        keystoreManager.savePin(pin)
        _uiState.update { it.copy(isPinSet = true, isAuthenticated = true) }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(isAuthenticated = true) }
    }

    fun onBiometricError(error: String) {
        _uiState.update { it.copy(error = error) }
    }
}
