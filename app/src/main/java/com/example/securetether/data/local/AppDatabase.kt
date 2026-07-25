package com.example.securetether.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.securetether.data.local.dao.VaultFileDao
import com.example.securetether.data.local.entity.VaultFileEntity

@Database(
    entities = [VaultFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultFileDao(): VaultFileDao
}
