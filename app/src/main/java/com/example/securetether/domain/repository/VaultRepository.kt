package com.example.securetether.domain.repository

import android.net.Uri
import com.example.securetether.domain.model.VaultFile
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    fun getAllFiles(): Flow<List<VaultFile>>
    suspend fun getFileById(fileId: String): VaultFile?
    fun searchFiles(query: String): Flow<List<VaultFile>>
    suspend fun addFile(file: VaultFile)
    suspend fun updateFile(file: VaultFile)
    suspend fun deleteFile(file: VaultFile)
    suspend fun importFile(uri: Uri): Result<Unit>
    suspend fun getDecryptedFile(fileId: String): Result<ByteArray>
    suspend fun exportFile(fileId: String): Result<Unit>
}
