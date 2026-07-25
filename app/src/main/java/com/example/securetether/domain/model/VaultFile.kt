package com.example.securetether.domain.model

data class VaultFile(
    val id: String,
    val displayName: String,
    val encryptedPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val addedAt: Long
)
