package fr.postgresjson.utils

import java.math.BigInteger
import java.security.MessageDigest

internal enum class Algorithm(name: String) {
    MD5("MD5")
}
internal fun String.hash(algorithm: Algorithm): String {
    val md = MessageDigest.getInstance(algorithm.name)
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}