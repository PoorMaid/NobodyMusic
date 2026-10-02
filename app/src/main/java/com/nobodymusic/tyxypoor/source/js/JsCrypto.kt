package com.nobodymusic.tyxypoor.source.js

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object JsCrypto {
    private val rnd = SecureRandom()

    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val d = md.digest(input.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
    }

    fun randomHex(bytes: Int): String {
        val b = ByteArray(bytes)
        rnd.nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    fun buffer(size: Int): String = " ".repeat(size.coerceIn(0, 65536))

    fun bufToString(value: String, fmt: String): String =
        when (fmt.lowercase()) {
            "base64" -> Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
            "hex" -> value.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
            else -> value
        }
}