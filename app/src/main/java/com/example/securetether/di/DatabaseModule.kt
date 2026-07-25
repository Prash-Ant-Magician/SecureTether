package com.example.securetether.di

import android.content.Context
import androidx.room.Room
import com.example.securetether.data.local.AppDatabase
import com.example.securetether.data.local.dao.VaultFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "secure_tether_vault_db"
        ).build()
    }

    @Provides
    fun provideVaultFileDao(database: AppDatabase): VaultFileDao {
        return database.vaultFileDao()
    }
}
