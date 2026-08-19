package com.example.securetether.ui.screens

import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.securetether.R
import com.example.securetether.domain.model.VaultFile
import com.example.securetether.ui.viewmodel.BluetoothViewModel

import com.example.securetether.ui.viewmodel.VaultUiState
import com.example.securetether.ui.viewmodel.VaultViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    bluetoothViewModel: BluetoothViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingDeleteUri by viewModel.pendingDeleteUri.collectAsStateWithLifecycle()
    val bluetoothState by bluetoothViewModel.state.collectAsStateWithLifecycle()

    var showDiscoveryDialog by remember { mutableStateOf(false) }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            showDiscoveryDialog = true
        }
    }

    VaultScreenContent(
        uiState = uiState,
        pendingDeleteUri = pendingDeleteUri,
        onNavigateToSettings = onNavigateToSettings,
        onImportFile = { viewModel.importFile(it) },
        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
        onViewFile = { viewModel.viewFile(it) },
        onCloseFile = { viewModel.closeFile() },
        onDeleteFile = { viewModel.deleteFile(it) },
        onExportFile = { viewModel.exportFile(it) },
        onClearExportMessage = { viewModel.clearExportMessage() },
        onDeletionPermissionHandled = { viewModel.onDeletionPermissionHandled() },
        loadThumbnail = { viewModel.loadThumbnail(it) },
        onShareBluetooth = {
            permissionLauncher.launch(bluetoothPermissions.toTypedArray())
        },
        modifier = modifier
    )

    if (showDiscoveryDialog) {
        DeviceDiscoveryDialog(
            scannedDevices = bluetoothState.scannedDevices,
            pairedDevices = bluetoothState.pairedDevices,
            isConnecting = bluetoothState.isConnecting,
            errorMessage = bluetoothState.errorMessage,
            onDeviceClick = { device ->
                bluetoothViewModel.connectToDevice(device)
            },
            onStartDiscovery = { bluetoothViewModel.startDiscovery() },
            onStopDiscovery = { bluetoothViewModel.stopDiscovery() },
            onDismiss = { 
                showDiscoveryDialog = false 
                bluetoothViewModel.clearError()
            }
        )
    }

    LaunchedEffect(bluetoothState.isConnected) {
        if (bluetoothState.isConnected && showDiscoveryDialog) {
            uiState.viewingFile?.let { file ->
                uiState.decryptedData?.let { data ->
                    bluetoothViewModel.sharePhoto(file.displayName, file.mimeType, data)
                }
            }
            showDiscoveryDialog = false
        }
    }

    bluetoothState.incomingPhoto?.let { photo ->
        SharedPhotoViewer(
            photo = photo,
            onClose = { bluetoothViewModel.clearIncomingPhoto() }
        )
    }

    // Start server automatically to listen for incoming shares
    LaunchedEffect(Unit) {
        bluetoothViewModel.startServer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreenContent(
    uiState: VaultUiState,
    pendingDeleteUri: Uri?,
    onNavigateToSettings: () -> Unit,
    onImportFile: (Uri) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onViewFile: (VaultFile) -> Unit,
    onCloseFile: () -> Unit,
    onDeleteFile: (VaultFile) -> Unit,
    onExportFile: (VaultFile) -> Unit,
    onClearExportMessage: () -> Unit,
    onDeletionPermissionHandled: () -> Unit,
    loadThumbnail: suspend (VaultFile) -> Bitmap?,
    onShareBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            onClearExportMessage()
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onImportFile(it) } }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onImportFile(it) } }

    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onDeletionPermissionHandled()
        }
    }

    LaunchedEffect(pendingDeleteUri) {
        pendingDeleteUri?.let { uri ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                    context.contentResolver,
                    listOf(uri)
                ).intentSender
                deletePermissionLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
            } else {
                onDeletionPermissionHandled()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Secure Vault", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { fileLauncher.launch("*/*") }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add File")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(4.dp))

            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { onSearchQueryChanged(it) },
                onSearch = { },
                active = false,
                onActiveChange = { },
                placeholder = { Text("Search your vault...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                windowInsets = WindowInsets(0.dp)
            ) { }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        photoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isImporting
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Image")
                }
            }

            if (uiState.files.isEmpty()) {
                EmptyVaultState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.files, key = { it.id }) { file ->
                        FileItem(
                            file = file,
                            onClick = { onViewFile(file) },
                            loadThumbnail = loadThumbnail
                        )
                    }
                }
            }
        }
    }

    if (uiState.viewingFile != null) {
        FileViewerDialog(
            file = uiState.viewingFile,
            decryptedData = uiState.decryptedData,
            isDecrypting = uiState.isDecrypting,
            error = uiState.error,
            onClose = onCloseFile,
            onDelete = { 
                onDeleteFile(uiState.viewingFile)
                onCloseFile()
            },
            onExport = { onExportFile(uiState.viewingFile) },
            onShareBluetooth = onShareBluetooth,
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
fun FileItem(
    file: VaultFile,
    onClick: () -> Unit,
    loadThumbnail: suspend (VaultFile) -> Bitmap?
) {
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    val isInspectionMode = androidx.compose.ui.platform.LocalInspectionMode.current
    var isVisible by remember { mutableStateOf(isInspectionMode) }

    LaunchedEffect(Unit) { if (!isInspectionMode) isVisible = true }
    LaunchedEffect(file.id) { thumbnail = loadThumbnail(file) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail!!.asImageBitmap(),
                            contentDescription = file.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = when {
                                file.mimeType.startsWith("image/") -> Icons.Rounded.Image
                                file.mimeType == "application/pdf" -> Icons.Rounded.PictureAsPdf
                                else -> Icons.Rounded.Description
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = truncateFileName(file.displayName),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = when {
                            file.mimeType.startsWith("image/") -> "IMG"
                            file.mimeType == "application/pdf" -> "PDF"
                            else -> "FILE"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerDialog(
    file: VaultFile,
    decryptedData: ByteArray?,
    isDecrypting: Boolean,
    error: String?,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onShareBluetooth: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(file.displayName) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (file.mimeType.startsWith("image/")) {
                            IconButton(onClick = onShareBluetooth) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share via Bluetooth",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onExport) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = "Export",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDecrypting -> CircularProgressIndicator()
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                    decryptedData != null -> {
                        if (file.mimeType.startsWith("image/")) {
                            ImageViewer(decryptedData)
                        } else if (file.mimeType == "application/pdf") {
                            PdfViewer(decryptedData)
                        } else {
                            Text("Preview not available for this file type")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete this file?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ImageViewer(data: ByteArray) {
    val bitmap = remember(data) { BitmapFactory.decodeByteArray(data, 0, data.size) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    } else {
        Text("Failed to load image")
    }
}

@Composable
fun PdfViewer(data: ByteArray) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var pdfError by remember { mutableStateOf<String?>(null) }

    remember(data) {
        try {
            val tempFile = File.createTempFile("pdf_preview", ".pdf", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(data) }
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bmp = android.graphics.Bitmap.createBitmap(
                    page.width, page.height, android.graphics.Bitmap.Config.ARGB_8888
                )
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap = bmp
                page.close()
            }
            renderer.close()
            pfd.close()
            tempFile.delete()
        } catch (e: Exception) {
            pdfError = "Failed to render PDF: ${e.message}"
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            pdfError != null -> Text(pdfError!!, color = MaterialTheme.colorScheme.error)
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            else -> CircularProgressIndicator()
        }
    }
}

@Composable
fun EmptyVaultState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Vault is empty",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Import files to keep them secure",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun truncateFileName(name: String): String {
    val maxLength = 16
    if (name.length <= maxLength) return name
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex == -1 || name.length - dotIndex > 5) return name.take(maxLength - 3) + "..."
    val extension = name.substring(dotIndex)
    val namePart = name.substring(0, dotIndex)
    val remainingSpace = maxLength - 3 - extension.length
    if (remainingSpace <= 0) return "..." + extension
    return namePart.take(remainingSpace) + "..." + extension
}


