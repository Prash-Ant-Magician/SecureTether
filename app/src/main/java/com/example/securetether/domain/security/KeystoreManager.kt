package com.example.securetether.domain.security

interface KeystoreManager {
    /**
     * Encrypts the given plaintext using the master key.
     * @return A byte array containing the IV followed by the ciphertext.
     */
    suspend fun encrypt(plaintext: ByteArray): ByteArray

    /**
     * Decrypts the given data (IV + ciphertext) using the master key.
     */
    suspend fun decrypt(encryptedData: ByteArray): ByteArray

    /**
     * Checks if a PIN has been set.
     */
    fun isPinSet(): Boolean

    /**
     * Saves the PIN securely.
     */
    fun savePin(pin: String)

    /**
     * Validates the given PIN against the saved one.
     */
    fun validatePin(pin: String): Boolean
}
