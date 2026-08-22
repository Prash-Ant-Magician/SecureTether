package com.example.securetether.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.ParcelUuid
import com.example.securetether.data.security.BluetoothSecurityHandler
import com.example.securetether.domain.model.BluetoothDeviceDomain
import com.example.securetether.domain.repository.BluetoothController
import com.example.securetether.domain.repository.BluetoothMessage
import com.example.securetether.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : BluetoothController {

    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverJob: Job? = null
    private val securityHandler = BluetoothSecurityHandler()

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private val bleScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }
    private val bleAdvertiser by lazy {
        bluetoothAdapter?.bluetoothLeAdvertiser
    }

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null
    private var isVerificationConfirmed = MutableStateFlow<Boolean?>(null)

    init {
        updatePairedDevices()
    }

    override fun verifyConnection(isVerified: Boolean) {
        isVerificationConfirmed.update { isVerified }
    }

    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.w("BluetoothController", "Missing BLUETOOTH_CONNECT permission to update paired devices")
            return
        }
        
        bluetoothAdapter?.bondedDevices?.map {
            BluetoothDeviceDomain(name = it.name, address = it.address)
        }?.let { devices ->
            _pairedDevices.update { devices }
        }
    }

    private var discoveryReceiver: BluetoothDeviceReceiver? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val scanRecord = result.scanRecord
            val serviceUuids = scanRecord?.serviceUuids
            
            if (serviceUuids?.contains(ParcelUuid.fromString(SERVICE_UUID)) == true) {
                _scannedDevices.update { devices ->
                    val newDevice = BluetoothDeviceDomain(
                        name = device.name ?: "Unknown",
                        address = device.address,
                        rssi = result.rssi
                    )
                    if (devices.none { it.address == newDevice.address }) {
                        devices + newDevice
                    } else {
                        devices.map { if (it.address == newDevice.address) newDevice else it }
                    }
                }
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("BluetoothController", "BLE Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BluetoothController", "BLE Advertising failed: $errorCode")
        }
    }

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN) || 
            !hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)) {
            Log.w("BluetoothController", "Missing BLE permissions")
            return
        }
        
        Log.d("BluetoothController", "Starting BLE discovery and advertising")
        
        // Start Scanning
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner?.startScan(listOf(filter), settings, scanCallback)

        // Start Advertising
        controllerScope.launch {
            val deviceName = settingsRepository.deviceName.first()
            val advertiseSettings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()

            val advertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                .build()

            bleAdvertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
        
        updatePairedDevices()
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        bleScanner?.stopScan(scanCallback)
        bleAdvertiser?.stopAdvertising(advertiseCallback)
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): SharedFlow<BluetoothMessage> {
        val flow = MutableSharedFlow<BluetoothMessage>()
        
        serverJob?.cancel()
        serverJob = controllerScope.launch {
            try {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    flow.emit(BluetoothMessage.TransferFailed("Missing BLUETOOTH_CONNECT permission"))
                    return@launch
                }

                if (bluetoothAdapter?.isEnabled != true) {
                    flow.emit(BluetoothMessage.TransferFailed("Bluetooth is disabled"))
                    return@launch
                }

                stopDiscovery()
                bluetoothAdapter?.cancelDiscovery()
                delay(100) // Give it a moment to stop discovery
                
                Log.d("BluetoothController", "Starting Bluetooth server")
                
                closeConnection()
                
                currentServerSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                    "SecureTether",
                    UUID.fromString(SERVICE_UUID)
                )

                while (isActive && currentServerSocket != null) {
                    val socket = try {
                        currentServerSocket?.accept()
                    } catch (e: IOException) {
                        if (isActive && currentServerSocket != null) {
                            Log.e("BluetoothController", "Server socket accept failed, recreating...", e)
                            currentServerSocket?.close()
                            delay(500)
                            currentServerSocket = try {
                                bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                                    "SecureTether",
                                    UUID.fromString(SERVICE_UUID)
                                )
                            } catch (recreateException: IOException) {
                                Log.e("BluetoothController", "Failed to recreate server socket", recreateException)
                                null
                            }
                            continue // Try again with the (potentially) new server socket
                        } else {
                            Log.d("BluetoothController", "Server socket closed or inactive")
                            break
                        }
                    } ?: break // Break if accept returns null (e.g. socket closed)
                    
                    Log.d("BluetoothController", "Server accepted connection from ${socket.remoteDevice.address}")
                    currentClientSocket = socket
                    
                    if (performHandshake(socket, flow, isServer = true)) {
                        _isConnected.update { true }
                        flow.emit(BluetoothMessage.ConnectionAccepted)
                        handleTransfer(socket, flow)
                    } else {
                        socket.close()
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e("BluetoothController", "Error in startBluetoothServer", e)
                    flow.emit(BluetoothMessage.TransferFailed(e.message ?: "Unknown error"))
                }
            } finally {
                _isConnected.update { false }
                currentServerSocket?.close()
                currentServerSocket = null
            }
        }

        return flow.asSharedFlow()
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): SharedFlow<BluetoothMessage> {
        val flow = MutableSharedFlow<BluetoothMessage>()
        
        serverJob?.cancel()
        controllerScope.launch {
            try {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    flow.emit(BluetoothMessage.TransferFailed("Missing BLUETOOTH_CONNECT permission"))
                    return@launch
                }

                if (bluetoothAdapter?.isEnabled != true) {
                    flow.emit(BluetoothMessage.TransferFailed("Bluetooth is disabled"))
                    return@launch
                }

                Log.d("BluetoothController", "Connecting to device: ${device.address}")
                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                
                stopDiscovery()
                closeConnection()

                currentClientSocket = bluetoothDevice?.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )
                
                currentClientSocket?.let { socket ->
                    try {
                        socket.connect()
                        Log.d("BluetoothController", "Connected to ${device.address}")
                        if (performHandshake(socket, flow, isServer = false)) {
                            _isConnected.update { true }
                            flow.emit(BluetoothMessage.ConnectionAccepted)
                            handleTransfer(socket, flow)
                        } else {
                            socket.close()
                            currentClientSocket = null
                        }
                    } catch (e: IOException) {
                        Log.e("BluetoothController", "Connection failed to ${device.address}", e)
                        socket.close()
                        currentClientSocket = null
                        flow.emit(BluetoothMessage.TransferFailed("Connection failed: ${e.localizedMessage}"))
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothController", "Error in connectToDevice", e)
                flow.emit(BluetoothMessage.TransferFailed(e.message ?: "Unknown error"))
            } finally {
                _isConnected.update { false }
            }
        }

        return flow.asSharedFlow()
    }

    private suspend fun performHandshake(
        socket: BluetoothSocket,
        flow: MutableSharedFlow<BluetoothMessage>,
        isServer: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dataInputStream = DataInputStream(socket.inputStream)
                val dataOutputStream = DataOutputStream(socket.outputStream)
                securityHandler.clear()

                // 1. Exchange Public Keys
                val localPublicKey = securityHandler.generateKeyPair()
                dataOutputStream.writeInt(localPublicKey.size)
                dataOutputStream.write(localPublicKey)
                dataOutputStream.flush()

                val remotePublicKeySize = dataInputStream.readInt()
                val remotePublicKey = ByteArray(remotePublicKeySize)
                dataInputStream.readFully(remotePublicKey)

                // 2. Compute Secret and SAS
                securityHandler.computeSharedSecret(remotePublicKey)
                val sas = securityHandler.generateSAS(remotePublicKey)
                val remoteName = socket.remoteDevice.name ?: "Unknown Device"
                
                flow.emit(BluetoothMessage.VerificationRequired(sas, remoteName))

                // 3. Wait for User Confirmation
                isVerificationConfirmed.value = null
                val confirmed = isVerificationConfirmed.first { it != null }
                
                // 4. Exchange Confirmation
                dataOutputStream.writeBoolean(confirmed == true)
                dataOutputStream.flush()
                
                val remoteConfirmed = dataInputStream.readBoolean()
                
                confirmed == true && remoteConfirmed
            } catch (e: Exception) {
                Log.e("BluetoothController", "Handshake failed", e)
                false
            }
        }
    }

    private suspend fun handleTransfer(socket: BluetoothSocket, flow: MutableSharedFlow<BluetoothMessage>) {
        withContext(Dispatchers.IO) {
            try {
                val dataInputStream = DataInputStream(socket.inputStream)
                
                // 1. Read Encrypted Metadata
                val metadataSize = dataInputStream.readInt()
                val encryptedMetadata = ByteArray(metadataSize)
                dataInputStream.readFully(encryptedMetadata)
                
                val decryptedMetadata = securityHandler.decrypt(encryptedMetadata)
                val metadataBuffer = ByteBuffer.wrap(decryptedMetadata)
                
                val fileNameLength = metadataBuffer.int
                val fileNameBytes = ByteArray(fileNameLength)
                metadataBuffer.get(fileNameBytes)
                val fileName = String(fileNameBytes)
                
                val mimeTypeLength = metadataBuffer.int
                val mimeTypeBytes = ByteArray(mimeTypeLength)
                metadataBuffer.get(mimeTypeBytes)
                val mimeType = String(mimeTypeBytes)
                
                val totalSize = metadataBuffer.long
                
                flow.emit(BluetoothMessage.MetadataReceived(fileName, mimeType, totalSize))
                
                // 2. Read Encrypted Chunks
                val fullData = ByteBuffer.allocate(totalSize.toInt())
                var bytesRead = 0L
                
                while (bytesRead < totalSize) {
                    val chunkSize = dataInputStream.readInt()
                    val encryptedChunk = ByteArray(chunkSize)
                    dataInputStream.readFully(encryptedChunk)
                    
                    val decryptedChunk = securityHandler.decrypt(encryptedChunk)
                    fullData.put(decryptedChunk)
                    bytesRead += decryptedChunk.size
                    
                    flow.emit(BluetoothMessage.ProgressUpdated(bytesRead, totalSize))
                }
                
                flow.emit(BluetoothMessage.TransferSucceeded(fullData.array()))
                
            } catch (e: Exception) {
                Log.e("BluetoothController", "Transfer failed", e)
                if (socket.isConnected) {
                    flow.emit(BluetoothMessage.TransferFailed("Transfer failed: ${e.message}"))
                }
            } finally {
                _isConnected.update { false }
                securityHandler.clear()
            }
        }
    }

    override suspend fun sendPhoto(fileName: String, mimeType: String, data: ByteArray): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val socket = currentClientSocket ?: return@withContext Result.failure(IOException("No active connection"))
                if (!socket.isConnected) return@withContext Result.failure(IOException("Socket not connected"))

                val dataOutputStream = DataOutputStream(socket.outputStream)
                
                // 1. Send Encrypted Metadata
                val fileNameBytes = fileName.toByteArray()
                val mimeTypeBytes = mimeType.toByteArray()
                val metadataSize = 4 + fileNameBytes.size + 4 + mimeTypeBytes.size + 8
                val metadata = ByteBuffer.allocate(metadataSize)
                    .putInt(fileNameBytes.size)
                    .put(fileNameBytes)
                    .putInt(mimeTypeBytes.size)
                    .put(mimeTypeBytes)
                    .putLong(data.size.toLong())
                    .array()
                
                val encryptedMetadata = securityHandler.encrypt(metadata)
                dataOutputStream.writeInt(encryptedMetadata.size)
                dataOutputStream.write(encryptedMetadata)
                dataOutputStream.flush()

                // 2. Send Encrypted Chunks
                val chunkSize = 32 * 1024 // 32KB
                var offset = 0
                while (offset < data.size) {
                    val length = minOf(chunkSize, data.size - offset)
                    val chunk = data.sliceArray(offset until offset + length)
                    val encryptedChunk = securityHandler.encrypt(chunk)
                    
                    dataOutputStream.writeInt(encryptedChunk.size)
                    dataOutputStream.write(encryptedChunk)
                    dataOutputStream.flush()
                    
                    offset += length
                    // Progress is handled by UI calling this, but we could add a flow here if needed
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("BluetoothController", "Send failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun closeConnection() {
        withContext(Dispatchers.IO) {
            currentClientSocket?.close()
            currentServerSocket?.close()
            currentClientSocket = null
            currentServerSocket = null
        }
    }

    override fun release() {
        controllerScope.cancel()
        discoveryReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Already unregistered or not registered
            }
        }
        discoveryReceiver = null
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val SERVICE_UUID = "8ce255c0-200a-11e0-ac64-0800200c9a66"
    }
}
