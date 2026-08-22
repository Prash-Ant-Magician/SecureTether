package com.example.securetether.domain.model

data class BluetoothDeviceDomain(
    val name: String?,
    val address: String,
    val deviceId: String? = null,
    val rssi: Int = 0,
    val isAvailable: Boolean = true
)
