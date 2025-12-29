package com.ckgod.kis.websokets

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesCrypto {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

    fun decrypt(key: String, iv: String, cipherText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), ALGORITHM)
        val ivSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        val decodedCipherText = Base64.getDecoder().decode(cipherText)
        val decryptedBytes = cipher.doFinal(decodedCipherText)

        return String(decryptedBytes, Charsets.UTF_8)
    }
}
