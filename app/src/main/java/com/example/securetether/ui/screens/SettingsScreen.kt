package com.example.securetether.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.securetether.R
import com.example.securetether.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategoryTitle("Security")
            
            ListItem(
                headlineContent = { Text("Change Vault PIN") },
                supportingContent = { Text("Update your 4-digit access code") },
                leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                modifier = Modifier.padding(vertical = 4.dp),
                trailingContent = {
                    TextButton(onClick = { showPinDialog = true }) {
                        Text("Change")
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Biometric Unlock") },
                supportingContent = { Text("Use fingerprint or face ID to unlock") },
                leadingContent = { Icon(Icons.Rounded.Fingerprint, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.biometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                }
            )

            AutoLockTimerSetting(
                currentValue = uiState.autoLockTimer,
                onValueChange = { viewModel.setAutoLockTimer(it) }
            )

            ListItem(
                headlineContent = { Text("Stealth Mode") },
                supportingContent = { Text("Hide app content from recent apps preview") },
                leadingContent = { Icon(Icons.Rounded.VisibilityOff, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.stealthModeEnabled,
                        onCheckedChange = { viewModel.setStealthModeEnabled(it) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsCategoryTitle("Appearance")

            ThemeSetting(
                currentMode = uiState.themeMode,
                onModeChange = { viewModel.setThemeMode(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsCategoryTitle("Storage")

            ListItem(
                headlineContent = { Text("Default Export Location") },
                supportingContent = { Text(uiState.exportPath) },
                leadingContent = { Icon(Icons.Rounded.Folder, contentDescription = null) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsCategoryTitle("About")

            ListItem(
                headlineContent = { Text("SecureTether") },
                supportingContent = { Text("Version ${uiState.version}") },
                leadingContent = { 
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground), 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    ) 
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPinDialog) {
        ChangePinDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { newPin ->
                viewModel.updatePin(newPin)
                showPinDialog = false
            },
            validateCurrentPin = { viewModel.validatePin(it) }
        )
    }
}

@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        fontWeight = FontWeight.Bold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoLockTimerSetting(
    currentValue: Long,
    onValueChange: (Long) -> Unit
) {
    val options = listOf(
        0L to "Never",
        30000L to "30 seconds",
        60000L to "1 minute",
        300000L to "5 minutes"
    )
    
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.find { it.first == currentValue }?.second ?: "Never"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        ListItem(
            headlineContent = { Text("Auto-Lock Timer") },
            supportingContent = { Text(currentLabel) },
            leadingContent = { Icon(Icons.Rounded.LockClock, contentDescription = null) },
            trailingContent = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSetting(
    currentMode: Int,
    onModeChange: (Int) -> Unit
) {
    val options = listOf("System default", "Light", "Dark")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        ListItem(
            headlineContent = { Text("Appearance") },
            supportingContent = { Text(options[currentMode]) },
            leadingContent = { Icon(Icons.Rounded.DarkMode, contentDescription = null) },
            trailingContent = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onModeChange(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    validateCurrentPin: (String) -> Boolean
) {
    var currentPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isCurrentPinVerified by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Vault PIN") },
        text = {
            Column {
                if (!isCurrentPinVerified) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { 
                            if (it.length <= 4) {
                                currentPin = it
                                error = null
                            }
                        },
                        label = { Text("Enter Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = error != null
                    )
                } else {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { 
                            if (it.length <= 4) {
                                pin = it
                                error = null
                            }
                        },
                        label = { Text("New 4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { 
                            if (it.length <= 4) {
                                confirmPin = it
                                error = null
                            }
                        },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = error != null
                    )
                }
                
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isCurrentPinVerified) {
                        if (currentPin.length != 4) {
                            error = "PIN must be 4 digits"
                        } else if (validateCurrentPin(currentPin)) {
                            isCurrentPinVerified = true
                            error = null
                        } else {
                            error = "Incorrect current PIN"
                        }
                    } else {
                        if (pin.length != 4) {
                            error = "PIN must be 4 digits"
                        } else if (pin != confirmPin) {
                            error = "PINs do not match"
                        } else {
                            onConfirm(pin)
                        }
                    }
                }
            ) {
                Text(if (!isCurrentPinVerified) "Next" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
