package com.example.securetether.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.securetether.domain.model.BluetoothDeviceDomain

@Composable
fun DeviceDiscoveryDialog(
    scannedDevices: List<BluetoothDeviceDomain>,
    pairedDevices: List<BluetoothDeviceDomain>,
    onDeviceClick: (BluetoothDeviceDomain) -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onDismiss: () -> Unit
) {
    DisposableEffect(Unit) {
        onStartDiscovery()
        onDispose {
            onStopDiscovery()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share via Bluetooth") },
        text = {
            Column {
                Text("Select a device to share the photo with.", style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Paired Devices", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(pairedDevices) { device ->
                        DeviceItem(device) { onDeviceClick(device) }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Scanned Devices", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(scannedDevices) { device ->
                        DeviceItem(device) { onDeviceClick(device) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onStartDiscovery) {
                Text("Refresh")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeviceItem(
    device: BluetoothDeviceDomain,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Column {
            Text(text = device.name ?: "Unknown Device", style = MaterialTheme.typography.bodyLarge)
            Text(text = device.address, style = MaterialTheme.typography.bodySmall)
        }
    }
}
