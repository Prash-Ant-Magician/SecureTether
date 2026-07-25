package com.example.securetether.data.local.dao

import androidx.room.*
import com.example.securetether.data.local.entity.VaultFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files ORDER BY addedAt DESC")
    fun getAllFiles(): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE id = :fileId")
    suspend fun getFileById(fileId: String): VaultFileEntity?

    @Query("SELECT * FROM vault_files WHERE displayName LIKE '%' || :query || '%' ORDER BY addedAt DESC")
    fun searchFiles(query: String): Flow<List<VaultFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFileEntity)

    @Update
    suspend fun updateFile(file: VaultFileEntity)

    @Delete
    suspend fun deleteFile(file: VaultFileEntity)
}
