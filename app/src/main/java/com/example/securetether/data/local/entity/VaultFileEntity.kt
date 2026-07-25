package com.example.securetether.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val encryptedPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val addedAt: Long
)
