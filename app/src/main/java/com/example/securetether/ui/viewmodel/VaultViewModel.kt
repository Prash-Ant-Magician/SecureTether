package com.example.securetether.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securetether.domain.model.VaultFile
import com.example.securetether.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val files: List<VaultFile> = emptyList(),
    val searchQuery: String = "",
    val isImporting: Boolean = false,
    val viewingFile: VaultFile? = null,
    val decryptedData: ByteArray? = null,
    val isDecrypting: Boolean = false,
    val error: String? = null,
    val exportMessage: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository
) : ViewModel() {

    private val thumbnailCache = LruCache<String, Bitmap>(50) // Cache for up to 50 thumbnails

    private val _searchQuery = MutableStateFlow("")
    private val _isImporting = MutableStateFlow(false)
    private val _viewingFile = MutableStateFlow<VaultFile?>(null)
    private val _decryptedData = MutableStateFlow<ByteArray?>(null)
    private val _isDecrypting = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _exportMessage = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<VaultUiState> = combine(
        repository.getAllFiles(),
        _searchQuery,
        _isImporting,
        _viewingFile,
        _decryptedData,
        _isDecrypting,
        _error,
        _exportMessage
    ) { args: Array<Any?> ->
        val files = args[0] as List<VaultFile>
        val query = args[1] as String
        val importing = args[2] as Boolean
        val viewing = args[3] as VaultFile?
        val decrypted = args[4] as ByteArray?
        val decrypting = args[5] as Boolean
        val error = args[6] as String?
        val exportMsg = args[7] as String?

        VaultUiState(
            files = if (query.isEmpty()) files else files.filter { it.displayName.contains(query, ignoreCase = true) },
            searchQuery = query,
            isImporting = importing,
            viewingFile = viewing,
            decryptedData = decrypted,
            isDecrypting = decrypting,
            error = error,
            exportMessage = exportMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private val _pendingDeleteUri = MutableStateFlow<Uri?>(null)
    val pendingDeleteUri: StateFlow<Uri?> = _pendingDeleteUri.asStateFlow()

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = repository.importFile(uri)
            result.fold(
                onSuccess = { 
                    _isImporting.value = false 
                },
                onFailure = { error ->
                    _isImporting.value = false
                    
                    when (error) {
                        is com.example.securetether.data.repository.MediaStorePermissionException -> {
                            // Use the resolved URI for the permission request
                            _pendingDeleteUri.value = error.uri
                        }
                        is android.app.RecoverableSecurityException -> {
                            _pendingDeleteUri.value = uri
                        }
                        else -> {
                            // Fallback for other MediaStore URIs if not already handled
                            if (uri.authority == "media" || uri.toString().contains("content://media/")) {
                                _pendingDeleteUri.value = uri
                            } else {
                                _error.value = "Import failed: ${error.message}"
                            }
                        }
                    }
                }
            )
        }
    }

    fun onDeletionPermissionHandled() {
        _pendingDeleteUri.value = null
    }

    fun importImage() {
        // This will be handled by the UI calling importFile with the result of a picker
    }

    fun importPdf() {
        // This will be handled by the UI calling importFile with the result of a picker
    }

    fun viewFile(file: VaultFile) {
        viewModelScope.launch {
            _viewingFile.value = file
            _isDecrypting.value = true
            _error.value = null
            
            repository.getDecryptedFile(file.id).fold(
                onSuccess = { _decryptedData.value = it },
                onFailure = { _error.value = "Failed to decrypt file: ${it.message}" }
            )
            
            _isDecrypting.value = false
        }
    }

    fun closeFile() {
        _viewingFile.value = null
        _decryptedData.value = null
        _error.value = null
    }

    fun deleteFile(file: VaultFile) {
        viewModelScope.launch {
            repository.deleteFile(file)
        }
    }

    fun exportFile(file: VaultFile) {
        viewModelScope.launch {
            val result = repository.exportFile(file.id)
            result.fold(
                onSuccess = {
                    val path = if (file.mimeType.startsWith("image/")) "Pictures/SecureTether" else "Documents/SecureTether"
                    _exportMessage.value = "Exported to $path"
                },
                onFailure = {
                    _error.value = "Export failed: ${it.message}"
                }
            )
        }
    }

    suspend fun getFileData(fileId: String): Result<ByteArray> {
        return repository.getDecryptedFile(fileId)
    }

    suspend fun loadThumbnail(file: VaultFile): Bitmap? {
        thumbnailCache.get(file.id)?.let { return it }
        
        if (!file.mimeType.startsWith("image/")) return null

        return repository.getDecryptedFile(file.id).fold(
            onSuccess = { data ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4 // Subsample for thumbnail performance
                }
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                if (bitmap != null) {
                    thumbnailCache.put(file.id, bitmap)
                }
                bitmap
            },
            onFailure = { null }
        )
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }
}
