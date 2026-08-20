package com.example.securetether.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securetether.domain.model.BluetoothDeviceDomain
import com.example.securetether.domain.repository.BluetoothController
import com.example.securetether.domain.repository.BluetoothMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val pairedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val incomingPhoto: SharedPhoto? = null
)

data class SharedPhoto(
    val fileName: String,
    val mimeType: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SharedPhoto
        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        bluetoothController.isConnected,
        _state
    ) { scannedDevices, pairedDevices, isConnected, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            isConnected = isConnected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun startDiscovery() {
        bluetoothController.startDiscovery()
    }

    fun stopDiscovery() {
        bluetoothController.stopDiscovery()
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        _state.update { it.copy(isConnecting = true, errorMessage = null) }
        bluetoothController.connectToDevice(device).onEach { message ->
            handleBluetoothMessage(message)
        }.launchIn(viewModelScope)
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothController.closeConnection()
            _state.update { it.copy(isConnected = false, isConnecting = false) }
        }
    }

    fun startServer() {
        bluetoothController.startBluetoothServer().onEach { message ->
            handleBluetoothMessage(message)
        }.launchIn(viewModelScope)
    }

    private var currentIncomingMetadata: Pair<String, String>? = null

    private fun handleBluetoothMessage(message: BluetoothMessage) {
        when (message) {
            is BluetoothMessage.MetadataReceived -> {
                currentIncomingMetadata = message.fileName to message.mimeType
            }
            is BluetoothMessage.TransferSucceeded -> {
                val metadata = currentIncomingMetadata
                if (metadata != null) {
                    _state.update { it.copy(
                        incomingPhoto = SharedPhoto(metadata.first, metadata.second, message.payload),
                        isConnected = true,
                        isConnecting = false
                    ) }
                }
            }
            is BluetoothMessage.TransferFailed -> {
                _state.update { it.copy(
                    errorMessage = message.message,
                    isConnecting = false,
                    isConnected = false
                ) }
            }
        }
    }

    fun sharePhoto(fileName: String, mimeType: String, data: ByteArray) {
        viewModelScope.launch {
            bluetoothController.sendPhoto(fileName, mimeType, data)
        }
    }

    fun clearIncomingPhoto() {
        _state.update { it.copy(incomingPhoto = null) }
        currentIncomingMetadata = null
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}
