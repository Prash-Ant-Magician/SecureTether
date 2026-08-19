package com.example.securetether.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.securetether.domain.model.BluetoothDeviceDomain
import com.example.securetether.ui.theme.SecureTetherTheme

@Composable
fun DeviceDiscoveryDialog(
    scannedDevices: List<BluetoothDeviceDomain>,
    pairedDevices: List<BluetoothDeviceDomain>,
    isConnecting: Boolean,
    errorMessage: String?,
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
            Box {
                Column {
                    Text("Select a device to share the photo with.", style = MaterialTheme.typography.bodyMedium)
                    
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Paired Devices", fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        items(pairedDevices) { device ->
                            DeviceItem(device, enabled = !isConnecting) { onDeviceClick(device) }
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Scanned Devices", fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        items(scannedDevices) { device ->
                            DeviceItem(device, enabled = !isConnecting) { onDeviceClick(device) }
                        }
                    }
                }

                if (isConnecting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = false) { }, // Block interaction
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onStartDiscovery, enabled = !isConnecting) {
                Text("Refresh")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isConnecting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeviceItem(
    device: BluetoothDeviceDomain,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.alpha(if (enabled) 1f else 0.5f)) {
            Text(text = device.name ?: "Unknown Device", style = MaterialTheme.typography.bodyLarge)
            Text(text = device.address, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceDiscoveryDialogPreview() {
    SecureTetherTheme {
        DeviceDiscoveryDialog(
            scannedDevices = listOf(
                BluetoothDeviceDomain("Device 1", "00:11:22:33:44:55"),
                BluetoothDeviceDomain("Device 2", "66:77:88:99:AA:BB")
            ),
            pairedDevices = listOf(
                BluetoothDeviceDomain("Paired 1", "CC:DD:EE:FF:00:11")
            ),
            isConnecting = false,
            errorMessage = null,
            onDeviceClick = {},
            onStartDiscovery = {},
            onStopDiscovery = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceDiscoveryDialogConnectingPreview() {
    SecureTetherTheme {
        DeviceDiscoveryDialog(
            scannedDevices = listOf(
                BluetoothDeviceDomain("Device 1", "00:11:22:33:44:55")
            ),
            pairedDevices = emptyList(),
            isConnecting = true,
            errorMessage = null,
            onDeviceClick = {},
            onStartDiscovery = {},
            onStopDiscovery = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceDiscoveryDialogErrorPreview() {
    SecureTetherTheme {
        DeviceDiscoveryDialog(
            scannedDevices = listOf(
                BluetoothDeviceDomain("Device 1", "00:11:22:33:44:55")
            ),
            pairedDevices = emptyList(),
            isConnecting = false,
            errorMessage = "Connection failed: Device unreachable",
            onDeviceClick = {},
            onStartDiscovery = {},
            onStopDiscovery = {},
            onDismiss = {}
        )
    }
}
