package com.musfit.data.remote.coach

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal enum class AiCoachEndpointRoute {
    Default,
    Loopback,
    PrivateLan,
}

internal data class ValidatedAiCoachEndpoint(
    val baseUrl: HttpUrl,
    val normalizedBaseUrl: String,
    val route: AiCoachEndpointRoute,
) {
    fun resolve(relativePath: String): HttpUrl {
        val segments = relativePath.trim('/').split('/').filter(String::isNotBlank)
        require(segments.none { it == "." || it == ".." }) { "Invalid AI coach request path." }
        return baseUrl.newBuilder().apply {
            segments.forEach(::addPathSegment)
        }.build()
    }
}

/**
 * Pure, build-variant-aware gate for every user-configured AI coach endpoint.
 *
 * This code deliberately performs no DNS or network lookup. Cleartext hostnames
 * are rejected rather than resolved because their address can change between
 * validation and dispatch. Android network-security XML cannot express private
 * CIDRs, so the internal variant's broad platform cleartext opt-in is safe only
 * in combination with this request-boundary gate.
 */
internal object AiCoachEndpointPolicy {
    fun requireAllowed(rawBaseUrl: String): ValidatedAiCoachEndpoint {
        val trimmed = rawBaseUrl.trim()
        val raw = parseStrictUrl(trimmed)
        val scheme = raw.scheme
        val rawHost = raw.host
        val route = rawHost.endpointRoute()

        if (scheme == "http") {
            if (!AI_COACH_PRIVATE_HTTP_ENABLED) {
                throw IllegalArgumentException("This build requires an HTTPS AI coach endpoint.")
            }
            if (route == AiCoachEndpointRoute.Default) {
                throw IllegalArgumentException(
                    "HTTP is allowed only for localhost or literal loopback/private IP endpoints. Use HTTPS otherwise.",
                )
            }
        }

        val parsed = raw.parsed
        val normalized = if (parsed.encodedPath.endsWith('/')) {
            parsed
        } else {
            parsed.newBuilder().addPathSegment("").build()
        }
        return ValidatedAiCoachEndpoint(
            baseUrl = normalized,
            normalizedBaseUrl = normalized.toString(),
            route = route,
        )
    }

    fun normalizedBaseUrlOrNull(rawBaseUrl: String): String? =
        runCatching { requireAllowed(rawBaseUrl).normalizedBaseUrl }.getOrNull()

    fun requiresPrivateLanRouting(rawBaseUrl: String): Boolean =
        AI_COACH_PRIVATE_ENDPOINT_ROUTING_ENABLED &&
            runCatching { requireAllowed(rawBaseUrl).route == AiCoachEndpointRoute.PrivateLan }
                .getOrDefault(false)

    private fun parseStrictUrl(value: String): StrictUrl {
        if (value.isBlank() || value.any(Char::isWhitespace)) invalidUrl()
        val schemeEnd = value.indexOf("://")
        if (schemeEnd <= 0) invalidUrl()
        val scheme = value.substring(0, schemeEnd).lowercase()
        if (scheme != "http" && scheme != "https") invalidUrl()
        val authorityStart = schemeEnd + 3
        if (authorityStart >= value.length) invalidUrl()
        val authorityEnd = value.indexOfAny(charArrayOf('/', '?', '#', '\\'), authorityStart)
            .let { if (it < 0) value.length else it }
        val authority = value.substring(authorityStart, authorityEnd)
        if (authority.isBlank() || authority.contains('@')) invalidUrl()

        val rawHost: String
        if (authority.startsWith('[')) {
            val bracketEnd = authority.indexOf(']')
            if (bracketEnd <= 1) invalidUrl()
            rawHost = authority.substring(1, bracketEnd)
            val remainder = authority.substring(bracketEnd + 1)
            if (remainder.isNotEmpty()) {
                if (!remainder.startsWith(':')) invalidUrl()
                validatePort(remainder.substring(1))
            }
        } else {
            if (authority.count { it == ':' } > 1) invalidUrl()
            val portSeparator = authority.lastIndexOf(':')
            rawHost = if (portSeparator >= 0) authority.substring(0, portSeparator) else authority
            if (portSeparator >= 0) validatePort(authority.substring(portSeparator + 1))
        }
        if (rawHost.isBlank() || rawHost.contains('%')) invalidUrl()
        val path = value.substring(authorityEnd)
        if (path.contains('?') || path.contains('#') || path.contains('\\')) invalidUrl()
        if (path.split('/').any { it == "." || it == ".." }) invalidUrl()
        if (
            (rawHost.contains('.') && rawHost.all { it.isDigit() || it == '.' } && rawHost.parseStrictIpv4() == null) ||
            rawHost.all(Char::isDigit) ||
            rawHost.startsWith("0x", ignoreCase = true)
        ) {
            invalidUrl()
        }
        val parsed = value.toHttpUrlOrNull() ?: invalidUrl()
        return StrictUrl(scheme = scheme, host = rawHost.lowercase(), parsed = parsed)
    }

    private fun validatePort(rawPort: String) {
        val port = rawPort.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            ?.toIntOrNull()
        if (port == null || port !in 1..65535) invalidUrl()
    }

    private fun String.endpointRoute(): AiCoachEndpointRoute {
        val host = lowercase()
        if (host == "localhost") return AiCoachEndpointRoute.Loopback
        host.parseStrictIpv4()?.let { octets ->
            return when {
                octets[0] == 127 -> AiCoachEndpointRoute.Loopback
                octets[0] == 10 -> AiCoachEndpointRoute.PrivateLan
                octets[0] == 172 && octets[1] in 16..31 -> AiCoachEndpointRoute.PrivateLan
                octets[0] == 192 && octets[1] == 168 -> AiCoachEndpointRoute.PrivateLan
                else -> AiCoachEndpointRoute.Default
            }
        }
        if (!host.contains(':')) return AiCoachEndpointRoute.Default
        val bytes = host.parseStrictIpv6() ?: invalidUrl()
        if (bytes.isIpv4Mapped()) return AiCoachEndpointRoute.Default
        if (bytes.dropLast(1).all { it == 0.toByte() } && bytes.last() == 1.toByte()) {
            return AiCoachEndpointRoute.Loopback
        }
        return if ((bytes[0].toInt() and 0xfe) == 0xfc) {
            AiCoachEndpointRoute.PrivateLan
        } else {
            AiCoachEndpointRoute.Default
        }
    }

    private fun String.parseStrictIpv4(): IntArray? {
        val parts = split('.')
        if (parts.size != 4) return null
        val octets = IntArray(4)
        for (index in parts.indices) {
            val part = parts[index]
            if (part.isEmpty() || part.length > 3 || (part.length > 1 && part.startsWith('0'))) return null
            if (!part.all(Char::isDigit)) return null
            octets[index] = part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
        }
        return octets
    }

    private fun String.parseStrictIpv6(): ByteArray? {
        if (contains('%') || contains('.')) return null
        val compressionIndex = indexOf("::")
        if (compressionIndex >= 0 && indexOf("::", compressionIndex + 2) >= 0) return null

        fun parseSide(side: String): List<Int>? {
            if (side.isEmpty()) return emptyList()
            return side.split(':').map { part ->
                if (part.isEmpty() || part.length > 4 || !part.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                    return null
                }
                part.toIntOrNull(16) ?: return null
            }
        }

        val leftText = if (compressionIndex >= 0) substring(0, compressionIndex) else this
        val rightText = if (compressionIndex >= 0) substring(compressionIndex + 2) else ""
        val left = parseSide(leftText) ?: return null
        val right = parseSide(rightText) ?: return null
        val missing = 8 - left.size - right.size
        val words = if (compressionIndex >= 0) {
            if (missing < 1) return null
            left + List(missing) { 0 } + right
        } else {
            if (left.size != 8) return null
            left
        }
        if (words.size != 8) return null
        return ByteArray(16).also { bytes ->
            words.forEachIndexed { index, word ->
                bytes[index * 2] = (word ushr 8).toByte()
                bytes[index * 2 + 1] = word.toByte()
            }
        }
    }

    private fun ByteArray.isIpv4Mapped(): Boolean =
        take(10).all { it == 0.toByte() } && this[10] == 0xff.toByte() && this[11] == 0xff.toByte()

    private data class StrictUrl(
        val scheme: String,
        val host: String,
        val parsed: HttpUrl,
    )

    private fun invalidUrl(): Nothing =
        throw IllegalArgumentException("Enter a valid http(s) base URL.")
}
