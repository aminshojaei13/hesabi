package com.braveboy.hesabi.core.hash

import java.security.MessageDigest

object Hashing {

    fun sha256(input: String): String = sha256(input.toByteArray(Charsets.UTF_8))

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun shortSha256(input: String): String = sha256(input).take(16)
}
