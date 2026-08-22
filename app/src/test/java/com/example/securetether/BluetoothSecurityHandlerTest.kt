package com.example.securetether

import com.example.securetether.data.security.BluetoothSecurityHandler
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BluetoothSecurityHandlerTest {

    @Test
    fun testKeyExchangeAndSharedSecretDerivation() {
        val alice = BluetoothSecurityHandler()
        val bob = BluetoothSecurityHandler()

        val alicePublicKey = alice.generateKeyPair()
        val bobPublicKey = bob.generateKeyPair()

        alice.computeSharedSecret(bobPublicKey)
        bob.computeSharedSecret(alicePublicKey)

        val aliceSas = alice.generateSAS(bobPublicKey)
        val bobSas = bob.generateSAS(alicePublicKey)

        assertEquals("SAS should match on both sides", aliceSas, bobSas)
    }

    @Test
    fun testEncryptionAndDecryption() {
        val alice = BluetoothSecurityHandler()
        val bob = BluetoothSecurityHandler()

        val alicePublicKey = alice.generateKeyPair()
        val bobPublicKey = bob.generateKeyPair()

        alice.computeSharedSecret(bobPublicKey)
        bob.computeSharedSecret(alicePublicKey)

        val plaintext = "Hello Secure Bluetooth!".toByteArray()
        val encrypted = alice.encrypt(plaintext)
        
        assertNotEquals("Encrypted data should not be same as plaintext", plaintext, encrypted)
        
        val decrypted = bob.decrypt(encrypted)
        assertArrayEquals("Decrypted data should match plaintext", plaintext, decrypted)
    }
}
