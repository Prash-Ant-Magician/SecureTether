package com.example.securetether.data.security

import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BluetoothSecurityHandler {

    companion object {
        private const val ECDH_ALGORITHM = "EC"
        private const val SHARED_SECRET_ALGORITHM = "AES"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BIT = 128
        private const val IV_LENGTH_BYTE = 12
        private const val KEY_SIZE = 256
    }

    private var keyPair: KeyPair? = null
    private var sharedSecret: ByteArray? = null
    private var sessionKey: SecretKeySpec? = null

    /**
     * Generates a new EC key pair for the session.
     */
    fun generateKeyPair(): ByteArray {
        val keyPairGenerator = KeyPairGenerator.getInstance(ECDH_ALGORITHM)
        keyPairGenerator.initialize(KEY_SIZE)
        val pair = keyPairGenerator.generateKeyPair()
        this.keyPair = pair
        return pair.public.encoded
    }

    /**
     * Computes the shared secret using the remote public key and derives a session key.
     */
    fun computeSharedSecret(remotePublicKeyBytes: ByteArray) {
        val keyFactory = KeyFactory.getInstance(ECDH_ALGORITHM)
        val publicKeySpec = X509EncodedKeySpec(remotePublicKeyBytes)
        val remotePublicKey = keyFactory.generatePublic(publicKeySpec)

        val keyAgreement = KeyAgreement.getInstance(ECDH_ALGORITHM)
        keyAgreement.init(keyPair?.private)
        keyAgreement.doPhase(remotePublicKey, true)

        val secret = keyAgreement.generateSecret()
        this.sharedSecret = secret

        // Derive AES key using SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        val derivedKey = digest.digest(secret)
        this.sessionKey = SecretKeySpec(derivedKey, SHARED_SECRET_ALGORITHM)
    }

    /**
     * Generates a numeric Short Authentication String (SAS) based on public keys and shared secret.
     */
    fun generateSAS(remotePublicKeyBytes: ByteArray): String {
        val localPublicKeyBytes = keyPair?.public?.encoded ?: return "000000"
        
        val combined = ByteBuffer.allocate(localPublicKeyBytes.size + remotePublicKeyBytes.size + (sharedSecret?.size ?: 0))
            .put(localPublicKeyBytes)
            .put(remotePublicKeyBytes)
            .apply { sharedSecret?.let { put(it) } }
            .array()

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined)
        
        // Take last 4 bytes and convert to a 6-digit number
        val numeric = ByteBuffer.wrap(hash.takeLast(4).toByteArray()).int
        return String.format("%06d", Math.abs(numeric % 1000000))
    }

    /**
     * Encrypts the payload using AES-GCM.
     */
    fun encrypt(payload: ByteArray): ByteArray {
        val key = sessionKey ?: throw IllegalStateException("Session key not generated")
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        
        val ciphertext = cipher.doFinal(payload)
        
        return ByteBuffer.allocate(iv.size + ciphertext.size)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    /**
     * Decrypts the payload using AES-GCM.
     */
    fun decrypt(encryptedPayload: ByteArray): ByteArray {
        val key = sessionKey ?: throw IllegalStateException("Session key not generated")
        if (encryptedPayload.size < IV_LENGTH_BYTE) throw IllegalArgumentException("Invalid encrypted payload")

        val iv = encryptedPayload.take(IV_LENGTH_BYTE).toByteArray()
        val ciphertext = encryptedPayload.drop(IV_LENGTH_BYTE).toByteArray()

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return cipher.doFinal(ciphertext)
    }
    
    fun clear() {
        keyPair = null
        sharedSecret = null
        sessionKey = null
    }
}
