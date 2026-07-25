package com.example.securetether.di

import com.example.securetether.data.security.KeystoreManagerImpl
import com.example.securetether.domain.security.KeystoreManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindKeystoreManager(
        keystoreManagerImpl: KeystoreManagerImpl
    ): KeystoreManager
}
