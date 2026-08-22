package com.example.securetether.domain.repository

import com.example.securetether.domain.model.BluetoothDeviceDomain
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val isConnected: StateFlow<Boolean>
    val errors: SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()

    fun startBluetoothServer(): SharedFlow<BluetoothMessage>
    fun connectToDevice(device: BluetoothDeviceDomain): SharedFlow<BluetoothMessage>
    suspend fun sendPhoto(fileName: String, mimeType: String, data: ByteArray): Result<Unit>
    fun verifyConnection(isVerified: Boolean)
    suspend fun closeConnection()
    fun release()
}

sealed interface BluetoothMessage {
    data class TransferSucceeded(val payload: ByteArray) : BluetoothMessage
    data class TransferFailed(val message: String) : BluetoothMessage
    data class MetadataReceived(val fileName: String, val mimeType: String, val fileSize: Long) : BluetoothMessage
    data class ProgressUpdated(val bytesSent: Long, val totalBytes: Long) : BluetoothMessage
    data class VerificationRequired(val code: String, val deviceName: String) : BluetoothMessage
    object ConnectionAccepted : BluetoothMessage
}
