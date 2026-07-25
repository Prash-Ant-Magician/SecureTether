package com.example.securetether.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.securetether.data.local.dao.VaultFileDao
import com.example.securetether.data.local.entity.VaultFileEntity
import com.example.securetether.domain.model.VaultFile
import com.example.securetether.domain.repository.VaultRepository
import com.example.securetether.domain.security.KeystoreManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class MediaStorePermissionException(val uri: Uri, cause: Throwable) : SecurityException(cause)

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val vaultFileDao: VaultFileDao,
    private val keystoreManager: KeystoreManager,
    @ApplicationContext private val context: Context
) : VaultRepository {

    companion object {
        private const val TAG = "VaultRepository"
    }

    override fun getAllFiles(): Flow<List<VaultFile>> {
        return vaultFileDao.getAllFiles().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getFileById(fileId: String): VaultFile? {
        return vaultFileDao.getFileById(fileId)?.toDomain()
    }

    override fun searchFiles(query: String): Flow<List<VaultFile>> {
        return vaultFileDao.searchFiles(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addFile(file: VaultFile) {
        vaultFileDao.insertFile(file.toEntity())
    }

    override suspend fun updateFile(file: VaultFile) {
        vaultFileDao.updateFile(file.toEntity())
    }

    override suspend fun deleteFile(file: VaultFile) {
        // Delete physical file first
        val encryptedFile = File(file.encryptedPath)
        if (encryptedFile.exists()) {
            encryptedFile.delete()
        }
        vaultFileDao.deleteFile(file.toEntity())
    }

    override suspend fun importFile(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Resolve metadata
            var displayName = "unknown"
            var size = 0L
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex)
                    size = cursor.getLong(sizeIndex)
                }
            }
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

            // 2. Read file bytes
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("Failed to open input stream"))

            // 3. Encrypt bytes
            val encryptedBytes = keystoreManager.encrypt(bytes)

            // 4. Save to app private storage
            val fileId = UUID.randomUUID().toString()
            val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
            val encryptedFile = File(vaultDir, fileId)
            FileOutputStream(encryptedFile).use { it.write(encryptedBytes) }

            // 5. Verify the saved copy is valid
            if (!encryptedFile.exists() || encryptedFile.length() == 0L) {
                return@withContext Result.failure(Exception("Failed to save encrypted file"))
            }

            // 6. Save metadata to Room
            val vaultFile = VaultFile(
                id = fileId,
                displayName = displayName,
                encryptedPath = encryptedFile.absolutePath,
                mimeType = mimeType,
                sizeBytes = size,
                addedAt = System.currentTimeMillis()
            )
            addFile(vaultFile)

            // 7. Delete original from public storage
            Log.d(TAG, "--- DELETION DEBUG START ---")
            Log.d(TAG, "URI: $uri")
            Log.d(TAG, "Authority: ${uri.authority}")
            Log.d(TAG, "Scheme: ${uri.scheme}")
            
            val isDocumentUri = DocumentsContract.isDocumentUri(context, uri)
            val isMediaDocumentsProvider = uri.authority == "com.android.providers.media.documents"
            val isMediaStoreUri = uri.authority == MediaStore.AUTHORITY || 
                                 uri.toString().contains("content://media/") ||
                                 uri.toString().contains("content://media/picker") ||
                                 isMediaDocumentsProvider
            
            Log.d(TAG, "--- DELETION DEBUG START ---")
            Log.d(TAG, "Original URI: $uri")
            Log.d(TAG, "Detection -> SAF: $isDocumentUri, MediaStore: $isMediaStoreUri, MediaDocs: $isMediaDocumentsProvider")

            try {
                if (isMediaStoreUri) {
                    var deleteUri = uri
                    
                    // Resolve to a standard MediaStore URI if possible
                    if (uri.toString().contains("content://media/picker")) {
                        try {
                            val mediaId = uri.lastPathSegment?.toLongOrNull()
                            if (mediaId != null) {
                                deleteUri = if (mimeType.startsWith("video/")) {
                                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaId)
                                } else {
                                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
                                }
                                Log.d(TAG, "Resolved Photo Picker URI to standard MediaStore URI: $deleteUri")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error resolving Picker URI: ${e.message}")
                        }
                    } else if (isMediaDocumentsProvider && isDocumentUri) {
                        try {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":")
                            if (split.size == 2) {
                                val type = split[0]
                                val id = split[1]
                                deleteUri = when (type) {
                                    "image" -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toLong())
                                    "video" -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toLong())
                                    "audio" -> ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong())
                                    else -> uri
                                }
                                Log.d(TAG, "Resolved SAF Media URI to standard MediaStore URI: $deleteUri")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error resolving MediaDocs URI: ${e.message}")
                        }
                    }

                    Log.d(TAG, "Attempting deletion on: $deleteUri")
                    try {
                        val deletedRows = contentResolver.delete(deleteUri, null, null)
                        Log.d(TAG, "Deletion success! Rows deleted: $deletedRows")
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Direct deletion failed for $deleteUri. This is expected on Android 10+ for files not owned by the app.")
                        
                        // We return a failure with a custom message that includes the deleteUri
                        // so the ViewModel can use the resolved URI for the permission request.
                        return@withContext Result.failure(MediaStorePermissionException(deleteUri, e))
                    }
                } else if (isDocumentUri) {
                    Log.w(TAG, "WARNING: SAF Document URI detected ($uri). Deletion is likely blocked by provider. Attempting DocumentsContract.deleteDocument() anyway...")
                    val deleted = DocumentsContract.deleteDocument(contentResolver, uri)
                    Log.d(TAG, "SAF deletion result: $deleted")
                } else {
                    Log.d(TAG, "Unknown URI type. Attempting generic contentResolver.delete().")
                    val deletedRows = contentResolver.delete(uri, null, null)
                    Log.d(TAG, "Generic deletion result: $deletedRows rows deleted")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "!!! DELETION SECURITY EXCEPTION !!!")
                Log.e(TAG, "Message: ${e.message}")
                
                if (isDocumentUri) {
                    Log.e(TAG, "FAILURE: SAF Document URIs (from GetContent) are read-only. Manual deletion required for this file.")
                    return@withContext Result.failure(Exception("Cannot delete original document: provider restriction. Please delete manually."))
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    Log.d(TAG, "RECOVERABLE: Surfacing deletion prompt to user via Activity Result API.")
                    return@withContext Result.failure(e)
                } else {
                    Log.e(TAG, "NON-RECOVERABLE: SecurityException for MediaStore URI.")
                    return@withContext Result.failure(e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during deletion: ${e.message}", e)
                return@withContext Result.failure(e)
            } finally {
                Log.d(TAG, "--- DELETION DEBUG END ---")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDecryptedFile(fileId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val vaultFileEntity = vaultFileDao.getFileById(fileId)
                ?: return@withContext Result.failure(Exception("File not found"))

            val file = File(vaultFileEntity.encryptedPath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("Encrypted file not found on disk"))
            }

            val encryptedBytes = file.readBytes()
            val decryptedBytes = keystoreManager.decrypt(encryptedBytes)

            Result.success(decryptedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportFile(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val vaultFile = vaultFileDao.getFileById(fileId)
                ?: return@withContext Result.failure(Exception("File not found"))

            val result = getDecryptedFile(fileId)
            val bytes = result.getOrNull() ?: return@withContext Result.failure(result.exceptionOrNull()!!)

            val contentResolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, vaultFile.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, vaultFile.mimeType)
            }

            val collectionUri: Uri
            if (vaultFile.mimeType.startsWith("image/")) {
                collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SecureTether")
                }
            } else {
                // For PDFs and other files, use Downloads or Documents
                collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/SecureTether")
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            }

            val uri = contentResolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun VaultFileEntity.toDomain() = VaultFile(
        id = id,
        displayName = displayName,
        encryptedPath = encryptedPath,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        addedAt = addedAt
    )

    private fun VaultFile.toEntity() = VaultFileEntity(
        id = id,
        displayName = displayName,
        encryptedPath = encryptedPath,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        addedAt = addedAt
    )
}
